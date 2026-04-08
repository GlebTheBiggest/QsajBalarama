// ========== User.java ==========
package com.qsaj.balarama.v1.model;

import com.qsaj.balarama.v1.model.enums.AppMode;
import com.qsaj.balarama.v1.model.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Загальний баланс (гроші, які можна витрачати)
    @Column(name = "saved_amount")
    private BigDecimal savedAmount;

    // Нерозподілені заощадження (накопичені з закритих періодів або додані вручну)
    @Column(name = "unallocated_savings")
    private BigDecimal unallocatedSavings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_mode", nullable = false)
    @Builder.Default
    private AppMode appMode = AppMode.CONTINUOUS;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (savedAmount == null) savedAmount = BigDecimal.ZERO;
        if (unallocatedSavings == null) unallocatedSavings = BigDecimal.ZERO;
        if (currency == null) currency = Currency.USD;
        if (appMode == null) appMode = AppMode.CONTINUOUS;
    }

    public long getDaysActive() {
        return ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }

    public String getTitle() {
        if (savedAmount == null) return "Novice";

        if (savedAmount.compareTo(new BigDecimal("5000")) >= 0) return "Gold Investor";
        if (savedAmount.compareTo(new BigDecimal("1000")) >= 0) return "Silver Saver";
        if (savedAmount.compareTo(new BigDecimal("100")) >= 0) return "Bronze Starter";
        return "Novice";
    }
}