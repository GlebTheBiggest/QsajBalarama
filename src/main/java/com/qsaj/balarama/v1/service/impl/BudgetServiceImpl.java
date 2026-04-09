package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.EventType;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.BudgetPeriodRepository;
import com.qsaj.balarama.v1.repository.CategoryBudgetRepository;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.service.NavigationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BudgetServiceImpl implements BudgetService {

    private final BudgetPeriodRepository periodRepo;
    private final CategoryBudgetRepository budgetRepo;
    private final UserRepository userRepo;
    private final FinanceService financeService;
    private final CategoryServiceImpl categoryService;
    private final TransactionRepository transactionRepo;
    private final NavigationService navigationService;
    private final EventService eventService;

    @Inject
    public BudgetServiceImpl(BudgetPeriodRepository periodRepo,
                             CategoryBudgetRepository budgetRepo,
                             UserRepository userRepo,
                             FinanceService financeService,
                             CategoryServiceImpl categoryService,
                             TransactionRepository transactionRepo,
                             NavigationService navigationService,
                             EventService eventService) {
        this.periodRepo = periodRepo;
        this.budgetRepo = budgetRepo;
        this.userRepo = userRepo;
        this.financeService = financeService;
        this.categoryService = categoryService;
        this.transactionRepo = transactionRepo;
        this.navigationService = navigationService;
        this.eventService = eventService;
    }

    @Override
    public BudgetPeriod getActivePeriod(User user) {
        return periodRepo.findActiveByUser(user).orElse(null);
    }

    @Override
    public List<BudgetPeriod> getAllPeriods(User user) {
        return periodRepo.findAllByUser(user);
    }

    @Override
    public BudgetPeriod startNewPeriod(User user, LocalDate startDate, LocalDate endDate) {
        closeActivePeriod(user);
        BudgetPeriod newPeriod = BudgetPeriod.builder()
                .user(user)
                .startDate(startDate)
                .endDate(endDate)
                .active(true)
                .build();
        periodRepo.save(newPeriod);
        eventService.logEvent(user, EventType.PERIOD_STARTED,
                "Started budget period: " + startDate + " – " + endDate);
        return newPeriod;
    }

    @Override
    public void recalculateSpentAmounts(BudgetPeriod period) {
        User user = period.getUser();
        List<Transaction> periodTransactions = transactionRepo.findAllByUser(user).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> t.getBudgetPeriod() != null && t.getBudgetPeriod().getId().equals(period.getId()))
                .toList();

        List<CategoryBudget> budgets = budgetRepo.findByPeriod(period);
        for (CategoryBudget budget : budgets) {
            CategoryGroup group = budget.getCategoryGroup();
            BigDecimal spent = periodTransactions.stream()
                    .filter(t -> t.getCategoryItem() != null && t.getCategoryItem().getGroup() != null)
                    .filter(t -> t.getCategoryItem().getGroup().getId().equals(group.getId()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            budget.setSpentAmount(spent);
            budgetRepo.update(budget);
        }
    }

    @Override
    public void closeActivePeriod(User user) {
        Optional<BudgetPeriod> activeOpt = periodRepo.findActiveByUser(user);
        if (activeOpt.isEmpty()) return;

        BudgetPeriod activePeriod = activeOpt.get();
        BigDecimal totalUnspent = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;
        boolean negative = totalUnspent.compareTo(BigDecimal.ZERO) < 0;
        if (negative) {
            boolean confirm = navigationService.showConfirmation(
                    "Negative Balance",
                    "Period closing will create a debt.",
                    "Your current balance is " + totalUnspent + ". This amount will be subtracted from your unallocated savings.\n" +
                            "Do you want to proceed?"
            );
            if (!confirm) return;
        }

        BigDecimal currentUnallocated = user.getUnallocatedSavings() != null ? user.getUnallocatedSavings() : BigDecimal.ZERO;
        user.setUnallocatedSavings(currentUnallocated.add(totalUnspent));
        user.setSavedAmount(BigDecimal.ZERO);
        userRepo.update(user);

        financeService.addSystemTransaction(
                user,
                negative ? "Debt from period " + activePeriod.getStartDate() + " – " + activePeriod.getEndDate() :
                        "Unspent from period " + activePeriod.getStartDate() + " – " + activePeriod.getEndDate(),
                totalUnspent.abs(),
                TransactionType.TRANSFER,
                LocalDate.now(),
                negative ? "Period closed with deficit, deducted from unallocated savings" : "Automatic transfer of all unspent money to unallocated savings"
        );

        activePeriod.setActive(false);
        periodRepo.update(activePeriod);

        eventService.logEvent(user, EventType.PERIOD_CLOSED,
                "Closed period: " + activePeriod.getStartDate() + " – " + activePeriod.getEndDate() +
                        ". Unspent: " + totalUnspent + " " + user.getCurrency().getSymbol());
    }

    @Override
    public void setCategoryBudget(BudgetPeriod period, CategoryGroup group, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            removeCategoryBudget(period, group);
            return;
        }

        Optional<CategoryBudget> existingOpt = budgetRepo.findByPeriodAndGroup(period, group);
        CategoryBudget budget;
        if (existingOpt.isPresent()) {
            budget = existingOpt.get();
        } else {
            budget = CategoryBudget.builder()
                    .budgetPeriod(period)
                    .categoryGroup(group)
                    .allocatedAmount(BigDecimal.ZERO)
                    .spentAmount(BigDecimal.ZERO)
                    .build();
            budgetRepo.save(budget);
        }

        if (budget.getSpentAmount().compareTo(amount) > 0) {
            throw new IllegalArgumentException("New budget limit cannot be less than already spent amount ("
                    + budget.getSpentAmount() + ").");
        }

        if (budget.getAllocatedAmount().compareTo(BigDecimal.ZERO) == 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
            User user = period.getUser();
            List<Transaction> periodTransactions = transactionRepo.findAllByUser(user).stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .filter(t -> t.getBudgetPeriod() != null && t.getBudgetPeriod().getId().equals(period.getId()))
                    .toList();
            BigDecimal spent = periodTransactions.stream()
                    .filter(t -> t.getCategoryItem() != null && t.getCategoryItem().getGroup() != null)
                    .filter(t -> t.getCategoryItem().getGroup().getId().equals(group.getId()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            budget.setSpentAmount(spent);
        }

        BigDecimal oldAmount = budget.getAllocatedAmount();
        budget.setAllocatedAmount(amount);
        budgetRepo.update(budget);

        if (oldAmount.compareTo(amount) != 0) {
            eventService.logEvent(period.getUser(), EventType.BUDGET_LIMIT_CHANGED,
                    "Changed budget for " + group.getName() + " from " + oldAmount + " to " + amount +
                            " " + period.getUser().getCurrency().getSymbol());
        }
    }

    @Override
    public void removeCategoryBudget(BudgetPeriod period, CategoryGroup group) {
        Optional<CategoryBudget> existingOpt = budgetRepo.findByPeriodAndGroup(period, group);
        existingOpt.ifPresent(budget -> {
            budgetRepo.delete(budget);
            eventService.logEvent(period.getUser(), EventType.BUDGET_LIMIT_CHANGED,
                    "Removed budget limit for " + group.getName());
        });
    }

    @Override
    public List<CategoryBudget> getBudgetsForPeriod(BudgetPeriod period) {
        return budgetRepo.findByPeriod(period);
    }

    @Override
    public void updatePeriod(BudgetPeriod period) {
        periodRepo.update(period);
        recalculateSpentAmounts(period);
        eventService.logEvent(period.getUser(), EventType.PERIOD_EXTENDED,
                "Extended period end date to: " + period.getEndDate());
    }
}