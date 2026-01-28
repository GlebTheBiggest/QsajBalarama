package com.qsaj.balarama.v1.model.enums;

public enum Currency {
    USD("$"),
    UAH("₴"),
    PLN("zł"),
    EUR("€");

    private final String symbol;

    Currency(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}