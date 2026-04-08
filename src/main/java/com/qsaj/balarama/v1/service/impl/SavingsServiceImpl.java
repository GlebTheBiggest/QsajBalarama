package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.Priority;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.SavingsGoalRepository;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.service.SavingsService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class SavingsServiceImpl implements SavingsService {
    private final SavingsGoalRepository savingsRepository;
    private final FinanceService financeService;
    private final CategoryServiceImpl categoryService;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Inject
    public SavingsServiceImpl(SavingsGoalRepository savingsRepository,
                              FinanceService financeService,
                              CategoryServiceImpl categoryService,
                              UserRepository userRepository,
                              TransactionRepository transactionRepository) {
        this.savingsRepository = savingsRepository;
        this.financeService = financeService;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void createGoal(User user, String name, BigDecimal targetAmount) {
        SavingsGoal goal = new SavingsGoal(name, targetAmount, user);
        savingsRepository.save(goal);

        CategoryGroup expGroup = getOrCreateGroup(user, "Savings", TransactionType.EXPENSE, "#e74c3c");
        categoryService.saveItem(CategoryItem.builder().name(name).group(expGroup).build());

        CategoryGroup incGroup = getOrCreateGroup(user, "Income", TransactionType.INCOME, "#27ae60");
        categoryService.saveItem(CategoryItem.builder().name(name).group(incGroup).build());
    }

    private CategoryGroup getOrCreateGroup(User user, String name, TransactionType type, String color) {
        CategoryGroup group = categoryService.getGroupsForUserByType(user, type).stream()
                .filter(g -> g.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (group == null) {
            group = CategoryGroup.builder().name(name).type(type).color(color).user(user).build();
            categoryService.saveGroup(group);
        }
        return group;
    }

    @Override
    public void addFundsToGoalFromBudget(User user, Long goalId, BigDecimal amount) {
        SavingsGoal goal = savingsRepository.findById(goalId);
        if (goal == null) throw new IllegalArgumentException("Goal not found");
        if (goal.isCompleted()) throw new IllegalArgumentException("Goal already completed");
        if (user.getSavedAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in main balance");
        }

        CategoryGroup savingsGroup = getOrCreateGroup(user, "Savings", TransactionType.EXPENSE, "#e74c3c");
        CategoryItem item = categoryService.findItemByGroupAndName(savingsGroup, goal.getName());
        if (item == null) {
            item = CategoryItem.builder().name(goal.getName()).group(savingsGroup).build();
            categoryService.saveItem(item);
        }

        financeService.addTransaction(user, "Add to goal from main: " + goal.getName(), amount,
                TransactionType.EXPENSE, item, LocalDate.now(),
                "Added from main balance", null, TransactionStatus.COMPLETED, Priority.STANDARD);
    }

    @Override
    public void addFundsToGoalFromUnallocated(User user, Long goalId, BigDecimal amount) {
        if (user.getUnallocatedSavings().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient unallocated savings!");
        }

        SavingsGoal goal = savingsRepository.findById(goalId);
        if (goal == null) throw new IllegalArgumentException("Goal not found");
        if (goal.isCompleted()) throw new IllegalArgumentException("Goal already completed");

        user.setUnallocatedSavings(user.getUnallocatedSavings().subtract(amount));
        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        goal.setTotalTransactions(goal.getTotalTransactions() + 1);
        savingsRepository.update(goal);
        userRepository.update(user);

        financeService.addSystemTransaction(user, "Transfer from unallocated to goal: " + goal.getName(),
                amount, TransactionType.TRANSFER, LocalDate.now(),
                "Moved from unallocated savings to envelope");
    }

    @Override
    public void transferFromGoalToUnallocated(User user, Long goalId, BigDecimal amount) {
        SavingsGoal goal = savingsRepository.findById(goalId);
        if (goal == null) throw new IllegalArgumentException("Goal not found");
        if (goal.getCurrentAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in goal");
        }

        goal.setCurrentAmount(goal.getCurrentAmount().subtract(amount));
        goal.setTotalWithdrawals(goal.getTotalWithdrawals().add(amount));
        user.setUnallocatedSavings(user.getUnallocatedSavings().add(amount));
        savingsRepository.update(goal);
        userRepository.update(user);

        financeService.addSystemTransaction(user, "Withdraw from goal to unallocated: " + goal.getName(),
                amount, TransactionType.TRANSFER, LocalDate.now(),
                "Withdrawn to unallocated savings");
    }

    @Override
    public void transferFromGoalToMain(User user, Long goalId, BigDecimal amount) {
        SavingsGoal goal = savingsRepository.findById(goalId);
        if (goal == null) throw new IllegalArgumentException("Goal not found");
        if (goal.getCurrentAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in goal");
        }

        financeService.addSystemTransaction(user, "Withdraw from goal to main: " + goal.getName(), amount,
                TransactionType.INCOME, LocalDate.now(), "Withdrawn to main balance");

        goal.setCurrentAmount(goal.getCurrentAmount().subtract(amount));
        goal.setTotalWithdrawals(goal.getTotalWithdrawals().add(amount));
        savingsRepository.update(goal);
    }

    @Override
    public void transferBetweenGoals(User user, Long fromGoalId, Long toGoalId, BigDecimal amount) {
        SavingsGoal fromGoal = savingsRepository.findById(fromGoalId);
        SavingsGoal toGoal = savingsRepository.findById(toGoalId);
        if (fromGoal == null || toGoal == null) {
            throw new IllegalArgumentException("One of the goals not found");
        }
        if (fromGoal.getCurrentAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in source goal");
        }
        if (toGoal.isCompleted()) {
            throw new IllegalArgumentException("Destination goal is already completed");
        }

        fromGoal.setCurrentAmount(fromGoal.getCurrentAmount().subtract(amount));
        toGoal.setCurrentAmount(toGoal.getCurrentAmount().add(amount));

        savingsRepository.update(fromGoal);
        savingsRepository.update(toGoal);

        financeService.addSystemTransaction(user, "Transfer between goals: " + fromGoal.getName() + " → " + toGoal.getName(),
                amount, TransactionType.TRANSFER, LocalDate.now(), "Internal transfer");
    }

    @Override
    public void completeGoal(Long goalId, User user, String note) {
        SavingsGoal goal = savingsRepository.findById(goalId);
        if (goal == null) throw new IllegalArgumentException("Goal not found");
        if (goal.isCompleted()) throw new IllegalArgumentException("Goal already completed");
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) < 0) {
            throw new IllegalArgumentException("Goal not reached yet");
        }

        List<Transaction> goalTransactions = transactionRepository.findAllByUser(user).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE
                        && t.getCategoryItem() != null
                        && t.getCategoryItem().getGroup() != null
                        && t.getCategoryItem().getGroup().getName().equalsIgnoreCase("Savings")
                        && t.getCategoryItem().getName().equalsIgnoreCase(goal.getName()))
                .collect(Collectors.toList());

        int totalTx = goalTransactions.size();
        BigDecimal totalWithdrawn = goal.getTotalWithdrawals();

        LocalDate firstDate = goalTransactions.stream()
                .map(Transaction::getDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        long days = ChronoUnit.DAYS.between(firstDate, LocalDate.now());

        goal.setCompleted(true);
        goal.setCompletionDate(LocalDate.now());
        goal.setCompletionNote(note);
        goal.setTotalTransactions(totalTx);
        goal.setTotalWithdrawals(totalWithdrawn);
        goal.setDaysToComplete((int) days);
        savingsRepository.update(goal);

        financeService.addSystemTransaction(user, "Goal completed: " + goal.getName(),
                goal.getTargetAmount(), TransactionType.TRANSFER, LocalDate.now(),
                "You've reached your goal! Note: " + note);
    }

    @Override
    public List<SavingsGoal> getUserGoals(User user) {
        return savingsRepository.findAllByUser(user);
    }

    @Override
    public List<SavingsGoal> getActiveGoals(User user) {
        return savingsRepository.findAllByUser(user).stream()
                .filter(g -> !g.isCompleted())
                .collect(Collectors.toList());
    }

    @Override
    public List<SavingsGoal> getCompletedGoals(User user) {
        return savingsRepository.findAllByUser(user).stream()
                .filter(SavingsGoal::isCompleted)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalSavingsInGoals(User user) {
        return savingsRepository.findAllByUser(user).stream()
                .map(SavingsGoal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalSavings(User user) {
        BigDecimal inGoals = getTotalSavingsInGoals(user);
        BigDecimal unallocated = user.getUnallocatedSavings() != null ? user.getUnallocatedSavings() : BigDecimal.ZERO;
        return inGoals.add(unallocated);
    }

    @Override
    public void deleteGoal(Long goalId, User user) {
        SavingsGoal goal = savingsRepository.findById(goalId);
        if (goal == null) return;

        if (goal.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0 && !goal.isCompleted()) {
            user.setUnallocatedSavings(user.getUnallocatedSavings().add(goal.getCurrentAmount()));
            userRepository.update(user);

            financeService.addSystemTransaction(user, "Goal refund: " + goal.getName(),
                    goal.getCurrentAmount(), TransactionType.TRANSFER, LocalDate.now(),
                    "Funds returned to unallocated savings due to goal deletion");
        }

        categoryService.deleteItemsByGroupNameAndItemName(user, "Savings", goal.getName());
        categoryService.deleteItemsByGroupNameAndItemName(user, "Income", goal.getName());

        savingsRepository.delete(goalId);
    }

    @Override
    public void transferFromUnallocatedToMain(User user, BigDecimal amount) {
        if (user.getUnallocatedSavings().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient unallocated savings!");
        }

        financeService.addSystemTransaction(user, "Transfer from unallocated to main", amount,
                TransactionType.INCOME, LocalDate.now(), "Moved from unallocated savings");

        user.setUnallocatedSavings(user.getUnallocatedSavings().subtract(amount));
        userRepository.update(user);
    }
}