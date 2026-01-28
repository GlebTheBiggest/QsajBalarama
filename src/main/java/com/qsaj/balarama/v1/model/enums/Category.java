package com.qsaj.balarama.v1.model.enums;

import lombok.Getter;

@Getter
public enum Category {
    // --- ВИТРАТИ (EXPENSE) ---
    FOOD(TransactionType.EXPENSE),
    TRANSPORT(TransactionType.EXPENSE),
    GAS(TransactionType.EXPENSE),
    WATER(TransactionType.EXPENSE),
    ELECTRICITY(TransactionType.EXPENSE),
    INTERNET(TransactionType.EXPENSE),
    MOBILE(TransactionType.EXPENSE),
    COOP_FEE(TransactionType.EXPENSE),
    CAR_REPAIR(TransactionType.EXPENSE),
    FUEL(TransactionType.EXPENSE),
    CLOTHING(TransactionType.EXPENSE),
    RENT(TransactionType.EXPENSE),
    ENTERTAINMENT(TransactionType.EXPENSE),
    SHOPPING(TransactionType.EXPENSE),
    HEALTH(TransactionType.EXPENSE),
    OTHER(TransactionType.EXPENSE),

    // --- ДОХОДИ (INCOME) ---
    SALARY(TransactionType.INCOME),
    FREELANCE(TransactionType.INCOME),
    GIFT(TransactionType.INCOME),
    INVESTMENT(TransactionType.INCOME);

    private final TransactionType type;

    Category(TransactionType type) {
        this.type = type;
    }
}
