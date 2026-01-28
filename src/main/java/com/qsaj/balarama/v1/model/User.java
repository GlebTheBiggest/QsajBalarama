package com.qsaj.balarama.v1.model;

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
@RequiredArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NonNull
    private String username;

    @Column(nullable = false)
    @NonNull
    private String password;

    @Column(unique = true, nullable = false)
    @NonNull
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "saved_amount")
    private BigDecimal savedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency = Currency.USD;

    public long getDaysActive() {
        return ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }

    public String getTitle() {
        if (savedAmount == null) return "Novice";
        double amount = savedAmount.doubleValue();

        if (amount >= 5000) return "Gold Investor";
        if (amount >= 1000) return "Silver Saver";
        if (amount >= 100) return "Bronze Starter";
        return "Novice";
    }
}