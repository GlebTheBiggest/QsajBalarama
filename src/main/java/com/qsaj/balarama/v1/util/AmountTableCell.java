package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import javafx.scene.control.TableCell;
import java.math.BigDecimal;

public class AmountTableCell extends TableCell<Transaction, BigDecimal> {
    private final String currencySymbol;

    public AmountTableCell(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    @Override
    protected void updateItem(BigDecimal amount, boolean empty) {
        super.updateItem(amount, empty);
        getStyleClass().removeAll("amount-income", "amount-expense", "amount-transfer");

        if (empty || amount == null || getTableRow() == null || getTableRow().getItem() == null) {
            setText(null);
        } else {
            Transaction t = getTableRow().getItem();
            String prefix;
            if (t.getType() == TransactionType.INCOME) {
                prefix = "+ ";
                getStyleClass().add("amount-income");
            } else if (t.getType() == TransactionType.EXPENSE) {
                prefix = "- ";
                getStyleClass().add("amount-expense");
            } else {
                prefix = "↺ ";
                getStyleClass().add("amount-transfer");
            }
            setText(prefix + currencySymbol + " " + amount);
        }
    }
}