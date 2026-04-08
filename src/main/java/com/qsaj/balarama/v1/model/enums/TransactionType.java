package com.qsaj.balarama.v1.model.enums;

public enum TransactionType {
    INCOME,   // Дохід (збільшує savedAmount)
    EXPENSE,  // Витрата (зменшує savedAmount)
    TRANSFER  // Внутрішній переказ (не впливає на баланс, лише для історії)
}