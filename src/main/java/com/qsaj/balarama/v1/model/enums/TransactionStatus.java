package com.qsaj.balarama.v1.model.enums;

public enum TransactionStatus {
    COMPLETED, // Вже оплачено / Отримано
    PENDING,   // Заплановано (ще не списано)
    OVERDUE    // Прострочено (для логіки відображення)
}