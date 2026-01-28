package com.qsaj.balarama.v1.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    public double getProgress() {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return currentAmount.doubleValue() / targetAmount.doubleValue();
    }
}