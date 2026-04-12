package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.Priority;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import com.qsaj.balarama.v1.service.MLService;
import smile.math.MathEx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MLServiceImpl implements MLService {

    private final TransactionRepository transactionRepository;

    @Inject
    public MLServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<MLInsight> generateInsights(User user, LocalDate startDate, LocalDate endDate) {
        List<MLInsight> insights = new ArrayList<>();
        List<Transaction> transactions = transactionRepository.findAllByUser(user).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
                .toList();

        if (transactions.isEmpty()) {
            insights.add(MLInsight.builder()
                    .type(MLInsight.InsightType.SPENDING_PATTERN)
                    .title("No Data")
                    .description("No expense transactions found for the selected period.")
                    .suggestion("Add some transactions to get personalized insights.")
                    .confidence(1.0)
                    .build());
            return insights;
        }

        insights.addAll(analyzeSpendingByCategory(transactions));
        insights.addAll(detectSeasonalTrends(transactions));
        insights.addAll(recommendBudgets(transactions));
        insights.addAll(analyzePriorities(transactions));
        insights.add(forecastNextMonth(transactions));
        insights.addAll(detectAnomalies(transactions));

        return insights.stream()
                .sorted(Comparator.comparing(MLInsight::getConfidence).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<MLInsight> analyzeSpendingByCategory(List<Transaction> transactions) {
        Map<String, BigDecimal> categorySpending = new HashMap<>();
        for (Transaction t : transactions) {
            String cat = t.getSnapshotGroupName() != null ? t.getSnapshotGroupName() : "Unknown";
            categorySpending.merge(cat, t.getAmount(), BigDecimal::add);
        }

        List<MLInsight> insights = new ArrayList<>();
        BigDecimal totalSpent = categorySpending.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Map.Entry<String, BigDecimal> entry : categorySpending.entrySet()) {
            String category = entry.getKey();
            BigDecimal amount = entry.getValue();
            double percentage = amount.divide(totalSpent, 4, RoundingMode.HALF_UP).doubleValue() * 100;

            if (percentage > 30) {
                insights.add(MLInsight.builder()
                        .type(MLInsight.InsightType.SPENDING_PATTERN)
                        .title("High spending in " + category)
                        .description(String.format("You spent %.1f%% of your total expenses on %s (%.2f %s).",
                                percentage, category, amount, transactions.get(0).getUser().getCurrency().getSymbol()))
                        .suggestion("Consider setting a budget limit for this category to control spending.")
                        .confidence(Math.min(percentage / 100, 0.95))
                        .categoryName(category)
                        .build());
            }
        }
        return insights;
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
        if (monthlySpending.size() >= 2) {
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
                    double change = lastMonthSpent.subtract(avgPrev).divide(avgPrev, 2, RoundingMode.HALF_UP).doubleValue() * 100;
                    if (Math.abs(change) > 20) {
                        insights.add(MLInsight.builder()
                                .type(MLInsight.InsightType.SEASONAL_TREND)
                                .title(change > 0 ? "Spending increased" : "Spending decreased")
                                .description(String.format("Last month spending changed by %.1f%% compared to average.", change))
                                .suggestion(change > 0 ? "Review recent large expenses." : "Great job saving money!")
                                .confidence(Math.min(Math.abs(change) / 100, 0.9))
                                .build());
                    }
                }
            }
        }
        return insights;
    }

    private List<MLInsight> recommendBudgets(List<Transaction> transactions) {
        Map<String, List<BigDecimal>> categoryAmounts = new HashMap<>();
        for (Transaction t : transactions) {
            String cat = t.getSnapshotGroupName() != null ? t.getSnapshotGroupName() : "Unknown";
            categoryAmounts.computeIfAbsent(cat, k -> new ArrayList<>()).add(t.getAmount());
        }

        List<MLInsight> insights = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : categoryAmounts.entrySet()) {
            String category = entry.getKey();
            List<BigDecimal> amounts = entry.getValue();
            if (amounts.size() >= 3) {
                double[] values = amounts.stream().mapToDouble(BigDecimal::doubleValue).toArray();
                double mean = MathEx.mean(values);
                double std = MathEx.sd(values);
                double recommended = mean + 0.5 * std;

                insights.add(MLInsight.builder()
                        .type(MLInsight.InsightType.BUDGET_RECOMMENDATION)
                        .title("Budget suggestion for " + category)
                        .description(String.format("Based on your history, average monthly spending is %.2f.", mean))
                        .suggestion(String.format("Set a budget of %.2f to have a comfortable cushion.", recommended))
                        .confidence(0.7)
                        .categoryName(category)
                        .predictedAmount(BigDecimal.valueOf(recommended))
                        .build());
            }
        }
        return insights;
    }

    private List<MLInsight> analyzePriorities(List<Transaction> transactions) {
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
                        .confidence(percentage / 100)
                        .build());
            }
        }
        return insights;
    }

    private MLInsight forecastNextMonth(List<Transaction> transactions) {
        if (transactions.size() < 6) {
            return MLInsight.builder()
                    .type(MLInsight.InsightType.FORECAST)
                    .title("Insufficient data for forecast")
                    .description("Need at least 6 months of data to predict next month spending.")
                    .suggestion("Keep adding transactions for better predictions.")
                    .confidence(0.5)
                    .build();
        }

        // Групуємо за місяцями
        Map<LocalDate, BigDecimal> monthlyTotal = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> LocalDate.of(t.getDate().getYear(), t.getDate().getMonth(), 1),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<LocalDate> dates = new ArrayList<>(monthlyTotal.keySet());
        dates.sort(LocalDate::compareTo);

        if (dates.size() < 3) {
            return MLInsight.builder()
                    .type(MLInsight.InsightType.FORECAST)
                    .title("Need more data")
                    .description("At least 3 months of data required for trend analysis.")
                    .suggestion("Track expenses for a few more months.")
                    .confidence(0.5)
                    .build();
        }

        // Проста лінійна регресія вручну (без залежності від Smile OLS)
        int n = dates.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i; // 0,1,2,... (індекс місяця)
            y[i] = monthlyTotal.get(dates.get(i)).doubleValue();
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }
        double b = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double a = (sumY - b * sumX) / n;

        double nextMonthIndex = n;
        double predicted = a + b * nextMonthIndex;

        // Обчислюємо залишки та довірчу ймовірність
        double[] residuals = new double[n];
        for (int i = 0; i < n; i++) {
            double fitted = a + b * x[i];
            residuals[i] = y[i] - fitted;
        }
        double mse = Arrays.stream(residuals).map(r -> r * r).average().orElse(0);
        double confidence = 1.0 / (1.0 + Math.sqrt(mse) / (Math.abs(predicted) + 1e-6));

        return MLInsight.builder()
                .type(MLInsight.InsightType.FORECAST)
                .title("Next month spending forecast")
                .description(String.format("Based on historical trend, predicted spending: %.2f", predicted))
                .suggestion(predicted > y[n-1] ? "Spending may increase. Consider reviewing your budget." :
                        "Spending may decrease. Keep up the good habits!")
                .confidence(Math.min(confidence, 0.85))
                .predictedAmount(BigDecimal.valueOf(predicted))
                .build();
    }

    private List<MLInsight> detectAnomalies(List<Transaction> transactions) {
        List<MLInsight> anomalies = new ArrayList<>();
        if (transactions.size() < 5) return anomalies;

        double[] amounts = transactions.stream()
                .mapToDouble(t -> t.getAmount().doubleValue())
                .toArray();
        double mean = MathEx.mean(amounts);
        double std = MathEx.sd(amounts);
        double threshold = mean + 2.5 * std;

        for (Transaction t : transactions) {
            if (t.getAmount().doubleValue() > threshold) {
                anomalies.add(MLInsight.builder()
                        .type(MLInsight.InsightType.ANOMALY_DETECTION)
                        .title("Unusually large expense")
                        .description(String.format("Transaction '%s' (%.2f) is significantly higher than average (%.2f).",
                                t.getTitle(), t.getAmount(), mean))
                        .suggestion("Verify if this expense is correct or consider splitting into smaller ones.")
                        .confidence(0.9)
                        .categoryName(t.getSnapshotGroupName())
                        .build());
                break;
            }
        }
        return anomalies;
    }
}