package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.Priority;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.AnalyticsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;

    @Inject
    public AnalyticsServiceImpl(TransactionRepository transactionRepository, BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
    }

    @Override
    public List<MLInsight> generateInsights(User user, LocalDate startDate, LocalDate endDate) {
        List<MLInsight> insights = new ArrayList<>();

        // 1. Отримуємо транзакції з бази. Якщо startDate та endDate == null (це All time)
        List<Transaction> allExpenseTransactions = transactionRepository.findAllByUser(user).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .toList();

        List<Transaction> transactionsToAnalyze;

        if (startDate == null || endDate == null) {
            // ЗАХИСТ ВІД OutOfMemory: Якщо вибрано All time, беремо тільки останні 12 місяців
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);
            transactionsToAnalyze = allExpenseTransactions.stream()
                    .filter(t -> !t.getDate().isBefore(oneYearAgo))
                    .collect(Collectors.toList());
        } else {
            // Фільтрація по вибраних датах
            transactionsToAnalyze = allExpenseTransactions.stream()
                    .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        // Якщо немає транзакцій - одразу повертаємо повідомлення
        if (transactionsToAnalyze.isEmpty()) {
            insights.add(MLInsight.builder()
                    .type(MLInsight.InsightType.SPENDING_PATTERN)
                    .title("No Data")
                    .description("No expense transactions found for the selected period.")
                    .suggestion("Add some transactions to get personalized insights.")
                    .confidence(1.0)
                    .build());
            return insights;
        }

        // --- Підготовка даних (Сумарні витрати по категоріях та Топ-3 категорії) ---
        Map<String, BigDecimal> categorySpending = new HashMap<>();
        for (Transaction t : transactionsToAnalyze) {
            String cat = t.getSnapshotGroupName() != null ? t.getSnapshotGroupName() : "Unknown";
            categorySpending.merge(cat, t.getAmount(), BigDecimal::add);
        }

        BigDecimal totalSpent = categorySpending.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // Беремо тільки ТОП-3 категорії за витратами, щоб не спамити
        List<String> top3Categories = categorySpending.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<String, Priority> categoryPriority = calculateCategoryPriority(transactionsToAnalyze);

        // --- Генерація інсайтів (БЕЗ обмежень по кількості транзакцій) ---

        // 1. Головний прогноз: Загальний бюджет на наступний місяць
        MLInsight totalForecast = forecastTotalBudget(transactionsToAnalyze);
        if (totalForecast != null) insights.add(totalForecast);

        // 2. Аналіз ТОП-3 категорій
        insights.addAll(analyzeSpendingByCategory(categorySpending, top3Categories, totalSpent));

        // 3. Рекомендації лімітів ТІЛЬКИ для ТОП-3 категорій
        insights.addAll(recommendCategoryLimits(categorySpending, top3Categories));

        // 4. Тренди та пріоритети
        insights.addAll(detectSeasonalTrends(transactionsToAnalyze));
        insights.addAll(analyzePriorities(transactionsToAnalyze, categoryPriority));

        // 5. Аномалії
        insights.addAll(detectAnomalies(transactionsToAnalyze));

        // Поради щодо перерозподілу активного бюджету (тільки якщо вказано період)
        if (startDate != null && endDate != null) {
            insights.addAll(suggestTransferFromLowPriority(user, transactionsToAnalyze, categoryPriority));
            insights.addAll(suggestBudgetIncrease(user, transactionsToAnalyze));
        }

        return insights.stream()
                .sorted(Comparator.comparing(MLInsight::getConfidence).reversed())
                .limit(12) // Ліміт на вивід найважливіших
                .collect(Collectors.toList());
    }

    private Map<String, Priority> calculateCategoryPriority(List<Transaction> transactions) {
        Map<String, Map<Priority, Integer>> categoryPriorityCount = new HashMap<>();
        for (Transaction t : transactions) {
            String cat = t.getSnapshotGroupName();
            if (cat == null) continue;
            categoryPriorityCount.computeIfAbsent(cat, k -> new HashMap<>())
                    .merge(t.getPriority(), 1, Integer::sum);
        }
        Map<String, Priority> result = new HashMap<>();
        for (Map.Entry<String, Map<Priority, Integer>> entry : categoryPriorityCount.entrySet()) {
            String cat = entry.getKey();
            Map<Priority, Integer> counts = entry.getValue();
            int essential = counts.getOrDefault(Priority.ESSENTIAL, 0);
            int standard = counts.getOrDefault(Priority.STANDARD, 0);
            int optional = counts.getOrDefault(Priority.OPTIONAL, 0);
            if (essential > standard && essential > optional) {
                result.put(cat, Priority.ESSENTIAL);
            } else if (optional > essential && optional > standard) {
                result.put(cat, Priority.OPTIONAL);
            } else {
                result.put(cat, Priority.STANDARD);
            }
        }
        return result;
    }

    private List<MLInsight> analyzeSpendingByCategory(Map<String, BigDecimal> categorySpending, List<String> topCategories, BigDecimal totalSpent) {
        List<MLInsight> insights = new ArrayList<>();
        if (totalSpent.compareTo(BigDecimal.ZERO) == 0) return insights;

        for (String cat : topCategories) {
            BigDecimal amount = categorySpending.get(cat);
            double percentage = amount.divide(totalSpent, 4, RoundingMode.HALF_UP).doubleValue() * 100;

            // Якщо категорія з'їдає більше 30% загальних витрат
            if (percentage > 30) {
                insights.add(MLInsight.builder()
                        .type(MLInsight.InsightType.SPENDING_PATTERN)
                        .title("High spending in " + cat)
                        .description(String.format("You spent %.1f%% of your total expenses on %s (%.2f).", percentage, cat, amount))
                        .suggestion("Keep an eye on this category as it takes up a major part of your expenses.")
                        .confidence(Math.min(percentage / 100.0, 0.95))
                        .categoryName(cat)
                        .build());
            }
        }
        return insights;
    }

    private List<MLInsight> recommendCategoryLimits(Map<String, BigDecimal> categorySpending, List<String> topCategories) {
        List<MLInsight> insights = new ArrayList<>();

        for (String cat : topCategories) {
            BigDecimal spent = categorySpending.get(cat);
            if (spent != null && spent.compareTo(BigDecimal.ZERO) > 0) {
                // Радимо ліміт = витрати + 10% буфера
                BigDecimal recommendedLimit = spent.multiply(BigDecimal.valueOf(1.10)).setScale(2, RoundingMode.HALF_UP);

                insights.add(MLInsight.builder()
                        .type(MLInsight.InsightType.BUDGET_RECOMMENDATION)
                        .title("Limit suggestion for " + cat)
                        .description(String.format("You spent %.2f on %s during this period.", spent, cat))
                        .suggestion(String.format("Consider setting a category limit of %.2f to have a comfortable buffer.", recommendedLimit))
                        .confidence(0.85)
                        .categoryName(cat)
                        .predictedAmount(recommendedLimit)
                        .build());
            }
        }
        return insights;
    }

    private MLInsight forecastTotalBudget(List<Transaction> transactions) {
        // Групуємо витрати по місяцях (незалежно від категорій)
        Map<YearMonth, BigDecimal> monthlyTotal = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getDate()),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<YearMonth> sortedMonths = new ArrayList<>(monthlyTotal.keySet());
        sortedMonths.sort(YearMonth::compareTo);

        if (sortedMonths.isEmpty()) return null;

        int n = sortedMonths.size();
        double predicted;
        double confidence;

        // Якщо маємо дані лише за 1 місяць, просто прогнозуємо ту ж саму суму
        if (n == 1) {
            predicted = monthlyTotal.get(sortedMonths.get(0)).doubleValue();
            confidence = 0.6; // Низька впевненість, бо мало даних
        } else {
            // Якщо є кілька місяців - використовуємо лінійну регресію для тренду
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            double[] y = new double[n];

            for (int i = 0; i < n; i++) {
                double xi = i;
                double yi = monthlyTotal.get(sortedMonths.get(i)).doubleValue();
                y[i] = yi;

                sumX += xi;
                sumY += yi;
                sumXY += xi * yi;
                sumX2 += xi * xi;
            }

            double meanX = sumX / n;
            double meanY = sumY / n;
            double denominator = (sumX2 - (sumX * sumX) / n);
            double slope = (denominator == 0) ? 0 : (sumXY - (sumX * sumY) / n) / denominator;
            double intercept = meanY - slope * meanX;

            predicted = intercept + slope * n;
            if (predicted < 0) predicted = 0;

            double[] residuals = new double[n];
            for (int i = 0; i < n; i++) {
                double fitted = intercept + slope * i;
                residuals[i] = y[i] - fitted;
            }
            double mse = Arrays.stream(residuals).map(r -> r * r).average().orElse(0);
            confidence = 1.0 / (1.0 + Math.sqrt(mse) / (Math.abs(predicted) + 1e-6));
        }

        // Рекомендований загальний бюджет = прогноз + 5% буфера
        BigDecimal recommendedTotalBudget = BigDecimal.valueOf(predicted * 1.05).setScale(2, RoundingMode.HALF_UP);

        return MLInsight.builder()
                .type(MLInsight.InsightType.FORECAST)
                .title("Total Budget Forecast")
                .description(String.format("Based on your overall spending habits, your estimated total expenses for the next month are %.2f.", predicted))
                .suggestion(String.format("Consider setting a total monthly budget of around %.2f to cover all your needs safely.", recommendedTotalBudget))
                .confidence(Math.min(confidence, 0.95))
                .predictedAmount(recommendedTotalBudget)
                .build();
    }

    private List<MLInsight> detectSeasonalTrends(List<Transaction> transactions) {
        Map<Integer, Map<Integer, BigDecimal>> monthlySpending = new HashMap<>();
        for (Transaction t : transactions) {
            int month = t.getDate().getMonthValue();
            int year = t.getDate().getYear();
            monthlySpending.computeIfAbsent(year, k -> new HashMap<>())
                    .merge(month, t.getAmount(), BigDecimal::add);
        }

        List<MLInsight> insights = new ArrayList<>();
        if (monthlySpending.size() >= 2 || (monthlySpending.size() == 1 && monthlySpending.values().iterator().next().size() >= 2)) {
            int lastYear = monthlySpending.keySet().stream().max(Integer::compareTo).orElse(0);
            if (lastYear > 0 && monthlySpending.get(lastYear) != null) {
                int lastMonth = monthlySpending.get(lastYear).keySet().stream().max(Integer::compareTo).orElse(0);
                BigDecimal lastMonthSpent = monthlySpending.get(lastYear).getOrDefault(lastMonth, BigDecimal.ZERO);

                List<BigDecimal> prevSpending = new ArrayList<>();
                for (Map.Entry<Integer, Map<Integer, BigDecimal>> yearEntry : monthlySpending.entrySet()) {
                    if (yearEntry.getKey() < lastYear || (yearEntry.getKey() == lastYear && yearEntry.getValue().containsKey(lastMonth - 1))) {
                        prevSpending.addAll(yearEntry.getValue().values());
                    }
                }
                if (!prevSpending.isEmpty()) {
                    BigDecimal avgPrev = prevSpending.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(prevSpending.size()), 2, RoundingMode.HALF_UP);

                    if (avgPrev.compareTo(BigDecimal.ZERO) > 0) {
                        double change = lastMonthSpent.subtract(avgPrev).divide(avgPrev, 2, RoundingMode.HALF_UP).doubleValue() * 100;
                        if (Math.abs(change) > 20) {
                            insights.add(MLInsight.builder()
                                    .type(MLInsight.InsightType.SEASONAL_TREND)
                                    .title(change > 0 ? "Spending increased" : "Spending decreased")
                                    .description(String.format("Last month spending changed by %.1f%% compared to your average.", change))
                                    .suggestion(change > 0 ? "Review recent large expenses." : "Great job saving money!")
                                    .confidence(Math.min(Math.abs(change) / 100.0, 0.9))
                                    .build());
                        }
                    }
                }
            }
        }
        return insights;
    }

    private List<MLInsight> analyzePriorities(List<Transaction> transactions, Map<String, Priority> categoryPriority) {
        Map<Priority, Long> priorityCount = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getPriority, Collectors.counting()));
        long total = transactions.size();
        List<MLInsight> insights = new ArrayList<>();

        for (Map.Entry<Priority, Long> entry : priorityCount.entrySet()) {
            double percentage = entry.getValue() * 100.0 / total;
            if (percentage > 50) {
                insights.add(MLInsight.builder()
                        .type(MLInsight.InsightType.PRIORITY_ANALYSIS)
                        .title(entry.getKey() + " transactions dominate")
                        .description(String.format("%.1f%% of your expenses are %s priority.", percentage, entry.getKey().name().toLowerCase()))
                        .suggestion(entry.getKey() == Priority.ESSENTIAL ?
                                "Essential expenses are stable. Focus on reducing optional spending." :
                                "Consider reviewing optional expenses to save more.")
                        .confidence(percentage / 100.0)
                        .build());
            }
        }
        return insights;
    }

    private List<MLInsight> detectAnomalies(List<Transaction> transactions) {
        List<MLInsight> anomalies = new ArrayList<>();
        double[] amounts = transactions.stream()
                .mapToDouble(t -> t.getAmount().doubleValue())
                .toArray();

        // Аномалію можна математично вирахувати тільки якщо є хоча б 3 значення (інакше дисперсія не показова)
        if (amounts.length <= 2) return anomalies;

        double mean = calculateMean(amounts);
        double std = calculateStdDev(amounts, mean);

        // Якщо всі транзакції однакові (std == 0), аномалій немає
        if (std == 0) return anomalies;

        double threshold = mean + 2.0 * std;

        for (Transaction t : transactions) {
            if (t.getAmount().doubleValue() > threshold) {
                anomalies.add(MLInsight.builder()
                        .type(MLInsight.InsightType.ANOMALY_DETECTION)
                        .title("Unusually large expense")
                        .description(String.format("Transaction '%s' (%.2f) is significantly higher than your average expense (%.2f).",
                                t.getTitle(), t.getAmount(), mean))
                        .suggestion("Verify if this expense is correct or consider splitting into smaller ones.")
                        .confidence(0.9)
                        .categoryName(t.getSnapshotGroupName())
                        .build());
                // Зупиняємось після першої знайденої аномалії, щоб не спамити список
                break;
            }
        }
        return anomalies;
    }

    private List<MLInsight> suggestTransferFromLowPriority(User user, List<Transaction> transactions, Map<String, Priority> categoryPriority) {
        List<MLInsight> insights = new ArrayList<>();
        BudgetPeriod activePeriod = budgetService.getActivePeriod(user);
        if (activePeriod == null) return insights;

        Map<String, CategoryBudget> budgets = budgetService.getBudgetsForPeriod(activePeriod).stream()
                .collect(Collectors.toMap(b -> b.getCategoryGroup().getName(), b -> b));

        Map<String, BigDecimal> spentMap = transactions.stream()
                .filter(t -> t.getDate().isAfter(activePeriod.getStartDate()) && t.getDate().isBefore(activePeriod.getEndDate()))
                .collect(Collectors.groupingBy(
                        t -> t.getSnapshotGroupName() != null ? t.getSnapshotGroupName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        for (Map.Entry<String, CategoryBudget> entry : budgets.entrySet()) {
            String category = entry.getKey();
            CategoryBudget budget = entry.getValue();
            BigDecimal allocated = budget.getAllocatedAmount();
            BigDecimal spent = spentMap.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal remaining = allocated.subtract(spent);

            if (allocated.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(allocated.multiply(BigDecimal.valueOf(0.2))) < 0) {
                Priority thisPriority = categoryPriority.getOrDefault(category, Priority.STANDARD);
                for (Map.Entry<String, CategoryBudget> other : budgets.entrySet()) {
                    String otherCat = other.getKey();
                    if (otherCat.equals(category)) continue;
                    Priority otherPriority = categoryPriority.getOrDefault(otherCat, Priority.STANDARD);
                    BigDecimal otherAllocated = other.getValue().getAllocatedAmount();
                    BigDecimal otherSpent = spentMap.getOrDefault(otherCat, BigDecimal.ZERO);
                    BigDecimal otherRemaining = otherAllocated.subtract(otherSpent);

                    if (otherRemaining.compareTo(BigDecimal.ZERO) > 0 && otherPriority.ordinal() < thisPriority.ordinal()) {
                        insights.add(MLInsight.builder()
                                .type(MLInsight.InsightType.BUDGET_RECOMMENDATION)
                                .title("Budget alert for " + category)
                                .description(String.format("Only %.2f left in %s limit.", remaining, category))
                                .suggestion(String.format("Consider moving funds from '%s' (surplus: %.2f, lower priority).", otherCat, otherRemaining))
                                .confidence(0.8)
                                .categoryName(category)
                                .predictedAmount(remaining.negate())
                                .build());
                        break; // Одна порада на таку ситуацію достатня
                    }
                }
            }
        }
        return insights;
    }

    private List<MLInsight> suggestBudgetIncrease(User user, List<Transaction> transactions) {
        List<MLInsight> insights = new ArrayList<>();
        BudgetPeriod activePeriod = budgetService.getActivePeriod(user);
        if (activePeriod == null) return insights;

        Map<String, CategoryBudget> budgets = budgetService.getBudgetsForPeriod(activePeriod).stream()
                .collect(Collectors.toMap(b -> b.getCategoryGroup().getName(), b -> b));

        Map<String, BigDecimal> spentMap = transactions.stream()
                .filter(t -> t.getDate().isAfter(activePeriod.getStartDate()) && t.getDate().isBefore(activePeriod.getEndDate()))
                .collect(Collectors.groupingBy(
                        t -> t.getSnapshotGroupName() != null ? t.getSnapshotGroupName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        for (Map.Entry<String, CategoryBudget> entry : budgets.entrySet()) {
            String category = entry.getKey();
            CategoryBudget budget = entry.getValue();
            BigDecimal allocated = budget.getAllocatedAmount();
            BigDecimal spent = spentMap.getOrDefault(category, BigDecimal.ZERO);

            if (allocated.compareTo(BigDecimal.ZERO) > 0 && spent.compareTo(allocated) > 0) {
                double overuse = spent.subtract(allocated).divide(allocated, 2, RoundingMode.HALF_UP).doubleValue() * 100;
                insights.add(MLInsight.builder()
                        .type(MLInsight.InsightType.BUDGET_RECOMMENDATION)
                        .title("Limit exceeded for " + category)
                        .description(String.format("You spent %.1f%% more than your set limit for %s.", overuse, category))
                        .suggestion(String.format("Consider increasing the limit for %s from %.2f to %.2f.", category, allocated, spent))
                        .confidence(0.85)
                        .categoryName(category)
                        .predictedAmount(spent)
                        .build());
            }
        }
        return insights;
    }

    // --- Допоміжні математичні методи ---
    private double calculateMean(double[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }

    private double calculateStdDev(double[] values, double mean) {
        if (values.length < 2) return 0.0;
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .sum() / (values.length - 1);
        return Math.sqrt(variance);
    }
}