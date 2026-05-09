package com.qsaj.balarama.v1.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Insight {
    private String title;
    private String description;
    private String suggestion;
    private InsightType type;
    private double confidence;
    private String categoryName;
    private BigDecimal predictedAmount;

    public enum InsightType {
        SPENDING_PATTERN,
        BUDGET_RECOMMENDATION,
        SEASONAL_TREND,
        ANOMALY_DETECTION,
        PRIORITY_ANALYSIS,
        FORECAST
    }
}