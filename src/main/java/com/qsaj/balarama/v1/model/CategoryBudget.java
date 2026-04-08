package com.qsaj.balarama.v1.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "category_budgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "budget_period_id", nullable = false)
    private BudgetPeriod budgetPeriod;

    @ManyToOne
    @JoinColumn(name = "category_group_id", nullable = false)
    private CategoryGroup categoryGroup;

    // Скільки грошей ти виділив на цю категорію на цей місяць (напр. 300)
    @Column(name = "allocated_amount", nullable = false)
    private BigDecimal allocatedAmount;

    // Скільки грошей ти вже витратив з цих 300
    @Column(name = "spent_amount", nullable = false)
    private BigDecimal spentAmount;

    @PrePersist
    protected void onCreate() {
        if (allocatedAmount == null) allocatedAmount = BigDecimal.ZERO;
        if (spentAmount == null) spentAmount = BigDecimal.ZERO;
    }

    // Зручний метод для отримання залишку
    public BigDecimal getRemainingAmount() {
        return allocatedAmount.subtract(spentAmount);
    }
}