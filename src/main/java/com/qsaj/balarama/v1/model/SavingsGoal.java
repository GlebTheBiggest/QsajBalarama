package com.qsaj.balarama.v1.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(nullable = false)
    private String name;

    @NonNull
    @Column(nullable = false)
    private BigDecimal targetAmount;

    @Column(nullable = false)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NonNull
    private User user;

    @Column(nullable = false)
    private boolean completed = false;

    private LocalDate completionDate;

    @Column(length = 500)
    private String completionNote;

    private Integer totalTransactions = 0;

    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    private Integer daysToComplete = 0;

    public double getProgress() {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        double progress = currentAmount.doubleValue() / targetAmount.doubleValue();
        return Math.min(progress, 1.0);
    }

    public boolean isReached() {
        return currentAmount.compareTo(targetAmount) >= 0;
    }
}