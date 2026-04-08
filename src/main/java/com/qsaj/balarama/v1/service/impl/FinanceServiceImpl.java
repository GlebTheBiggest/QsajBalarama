package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.repository.*;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.FinanceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FinanceServiceImpl implements FinanceService {
    private final TransactionRepository txRepo;
    private final UserRepository userRepo;
    private final BudgetService budgetService;
    private final CategoryBudgetRepository categoryBudgetRepo;
    private final SavingsGoalRepository savingsGoalRepo;
    private final CategoryGroupRepository categoryGroupRepo;
    private final CategoryItemRepository categoryItemRepository;

    @Inject
    public FinanceServiceImpl(TransactionRepository txRepo,
                              UserRepository userRepo,
                              BudgetService budgetService,
                              CategoryBudgetRepository categoryBudgetRepo,
                              SavingsGoalRepository savingsGoalRepo,
                              CategoryGroupRepository categoryGroupRepo,
                              CategoryItemRepository categoryItemRepository) {
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.budgetService = budgetService;
        this.categoryBudgetRepo = categoryBudgetRepo;
        this.savingsGoalRepo = savingsGoalRepo;
        this.categoryGroupRepo = categoryGroupRepo;
        this.categoryItemRepository = categoryItemRepository;
    }

    @Override
    public void addTransaction(User user, String title, BigDecimal amount, TransactionType type,
                               CategoryItem categoryItem, LocalDate date, String notes,
                               LocalDate dueDate, TransactionStatus status, Priority priority) {

        if (user.getAppMode() == AppMode.PERIODIC && type != TransactionType.TRANSFER && budgetService.getActivePeriod(user) == null) {
            throw new IllegalArgumentException("Cannot add transaction: No active budget period. Please start a period first.");
        }

        boolean isGoalWithdrawal = type == TransactionType.INCOME && categoryItem != null &&
                categoryItem.getGroup() != null && categoryItem.getGroup().getName().equalsIgnoreCase("Income");
        if (isGoalWithdrawal) {
            SavingsGoal goal = savingsGoalRepo.findAllByUser(user).stream()
                    .filter(g -> g.getName().equalsIgnoreCase(categoryItem.getName()))
                    .findFirst().orElse(null);
            if (goal != null && goal.getCurrentAmount().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient funds in envelope '" + goal.getName() +
                        "'. Available: " + goal.getCurrentAmount());
            }
        }

        if (user.getAppMode() == AppMode.PERIODIC && type == TransactionType.EXPENSE &&
                categoryItem != null && categoryItem.getGroup() != null &&
                !categoryItem.getGroup().getName().equalsIgnoreCase("Savings") &&
                !categoryItem.getGroup().getName().equalsIgnoreCase("System")) {
            BudgetPeriod activePeriod = budgetService.getActivePeriod(user);
            if (activePeriod != null) {
                CategoryBudget budget = categoryBudgetRepo.findByPeriodAndGroup(activePeriod, categoryItem.getGroup())
                        .orElse(null);
                if (budget != null && budget.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal newSpent = budget.getSpentAmount().add(amount);
                    if (newSpent.compareTo(budget.getAllocatedAmount()) > 0) {
                        throw new IllegalArgumentException("Exceeds budget limit for " + categoryItem.getGroup().getName() +
                                ". Limit: " + budget.getAllocatedAmount() + ", spent: " + budget.getSpentAmount() +
                                ", attempted: " + amount);
                    }
                }
            }
        }

        Transaction tx = Transaction.builder()
                .title(title).amount(amount).type(type).categoryItem(categoryItem)
                .date(date).user(user).notes(notes).dueDate(dueDate)
                .status(status).priority(priority).build();

        if (user.getAppMode() == AppMode.PERIODIC && type != TransactionType.TRANSFER) {
            BudgetPeriod activePeriod = budgetService.getActivePeriod(user);
            tx.setBudgetPeriod(activePeriod);
        }

        txRepo.save(tx);

        if (type != TransactionType.TRANSFER && status != null) {
            applyBalance(user, amount, type, false, categoryItem);
        }

        if (type != TransactionType.TRANSFER) {
            syncSavingsGoalAfterTransaction(user, type, categoryItem, amount, false);
        }
    }

    @Override
    public void addSystemTransaction(User user, String title, BigDecimal amount, TransactionType type,
                                     LocalDate date, String notes) {
        CategoryGroup sysGroup = categoryGroupRepo.findAllByUser(user).stream()
                .filter(g -> g.getName().equalsIgnoreCase("System") && g.getType() == type)
                .findFirst().orElse(null);
        if (sysGroup == null) {
            sysGroup = CategoryGroup.builder()
                    .name("System")
                    .type(type)
                    .color("#3498db")
                    .user(user)
                    .build();
            categoryGroupRepo.save(sysGroup);
        }

        CategoryItem sysItem = categoryItemRepository.findAllByGroupId(sysGroup.getId()).stream()
                .filter(i -> i.getName().equalsIgnoreCase("Unallocated Transfer"))
                .findFirst().orElse(null);
        if (sysItem == null) {
            sysItem = CategoryItem.builder()
                    .name("Unallocated Transfer")
                    .group(sysGroup)
                    .build();
            categoryItemRepository.save(sysItem);
        }

        Transaction tx = Transaction.builder()
                .title(title).amount(amount).type(type)
                .categoryItem(sysItem)
                .date(date).user(user).notes(notes)
                .status(TransactionStatus.COMPLETED)
                .priority(Priority.STANDARD)
                .build();

        if (user.getAppMode() == AppMode.PERIODIC) {
            BudgetPeriod activePeriod = budgetService.getActivePeriod(user);
            tx.setBudgetPeriod(activePeriod);
        }

        txRepo.save(tx);
    }

    @Override
    public List<Transaction> getUserTransactions(User user) {
        return txRepo.findAllByUser(user);
    }

    @Override
    public void deleteTransaction(Long id, User user) {
        Transaction tx = txRepo.findById(id);
        if (tx == null) return;

        if (tx.getType() != TransactionType.TRANSFER) {
            applyBalance(user, tx.getAmount(), tx.getType(), true, tx.getCategoryItem());
            syncSavingsGoalAfterTransaction(user, tx.getType(), tx.getCategoryItem(), tx.getAmount(), true);
        }
        txRepo.delete(id);
    }

    @Override
    public void deleteTransactionWithoutRefund(Long id, User user) {
        Transaction tx = txRepo.findById(id);
        if (tx == null) return;
        txRepo.delete(id);
    }

    private void syncSavingsGoalAfterTransaction(User user, TransactionType type,
                                                 CategoryItem categoryItem, BigDecimal amount, boolean reverse) {
        if (categoryItem == null || categoryItem.getGroup() == null) return;

        boolean isSavingsExpense = type == TransactionType.EXPENSE &&
                categoryItem.getGroup().getName().equalsIgnoreCase("Savings");
        boolean isGoalWithdrawal = type == TransactionType.INCOME &&
                categoryItem.getGroup().getName().equalsIgnoreCase("Income");

        if (isSavingsExpense || isGoalWithdrawal) {
            SavingsGoal goal = savingsGoalRepo.findAllByUser(user).stream()
                    .filter(g -> g.getName().equalsIgnoreCase(categoryItem.getName()))
                    .findFirst().orElse(null);
            if (goal != null) {
                if (isSavingsExpense) {
                    goal.setCurrentAmount(reverse ? goal.getCurrentAmount().subtract(amount) :
                            goal.getCurrentAmount().add(amount));
                } else {
                    goal.setCurrentAmount(reverse ? goal.getCurrentAmount().add(amount) :
                            goal.getCurrentAmount().subtract(amount));
                }
                savingsGoalRepo.update(goal);
            }
        }
    }

    private void applyBalance(User user, BigDecimal amount, TransactionType type, boolean reverse, CategoryItem categoryItem) {
        BigDecimal currentBalance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;
        BigDecimal currentUnallocated = user.getUnallocatedSavings() != null ? user.getUnallocatedSavings() : BigDecimal.ZERO;

        boolean isSavingsExpense = type == TransactionType.EXPENSE && categoryItem != null &&
                categoryItem.getGroup() != null && categoryItem.getGroup().getName().equalsIgnoreCase("Savings");
        boolean isGoalWithdrawal = type == TransactionType.INCOME && categoryItem != null &&
                categoryItem.getGroup() != null && categoryItem.getGroup().getName().equalsIgnoreCase("Income") &&
                savingsGoalRepo.findAllByUser(user).stream().anyMatch(g -> g.getName().equalsIgnoreCase(categoryItem.getName()));
        boolean isSystemTransfer = categoryItem != null && categoryItem.getGroup() != null &&
                categoryItem.getGroup().getName().equalsIgnoreCase("System");

        if (isSavingsExpense) {
            if (reverse) user.setSavedAmount(currentBalance.add(amount));
            else user.setSavedAmount(currentBalance.subtract(amount));
        } else if (isGoalWithdrawal) {
            if (reverse) user.setUnallocatedSavings(currentUnallocated.subtract(amount));
            else user.setUnallocatedSavings(currentUnallocated.add(amount));
        } else if (isSystemTransfer) {
            return;
        } else {
            if (type == TransactionType.INCOME) {
                user.setSavedAmount(reverse ? currentBalance.subtract(amount) : currentBalance.add(amount));
            } else if (type == TransactionType.EXPENSE) {
                user.setSavedAmount(reverse ? currentBalance.add(amount) : currentBalance.subtract(amount));
            }
        }

        if (user.getAppMode() == AppMode.PERIODIC && type == TransactionType.EXPENSE &&
                !isSavingsExpense && !isGoalWithdrawal && !isSystemTransfer &&
                categoryItem != null && categoryItem.getGroup() != null) {
            updateCategoryBudget(user, categoryItem.getGroup(), amount, reverse);
        }

        userRepo.update(user);
    }

    private void updateCategoryBudget(User user, CategoryGroup group, BigDecimal amount, boolean reverse) {
        BudgetPeriod activePeriod = budgetService.getActivePeriod(user);
        if (activePeriod != null && group != null) {
            Optional<CategoryBudget> budgetOpt = categoryBudgetRepo.findByPeriodAndGroup(activePeriod, group);
            if (budgetOpt.isPresent()) {
                CategoryBudget budget = budgetOpt.get();
                if (budget.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentSpent = budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO;
                    budget.setSpentAmount(reverse ? currentSpent.subtract(amount) : currentSpent.add(amount));
                    categoryBudgetRepo.update(budget);
                }
            }
        }
    }
}