package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

import java.math.BigDecimal;

public class AmountTableCell extends TableCell<Transaction, BigDecimal> {
    private final String currencySymbol;

    public AmountTableCell() {
        this.currencySymbol = CurrencyFormatter.getDefaultSymbol();
    }

    @Override
    protected void updateItem(BigDecimal amount, boolean empty) {
        super.updateItem(amount, empty);
        if (empty || amount == null) {
            setText(null);
            setStyle("");
        } else {
            Transaction t = getTableView().getItems().get(getIndex());
            String prefix = (t.getType() == TransactionType.INCOME) ? "+ " : "- ";
            setText(prefix + currencySymbol + " " + amount);

            if (t.getType() == TransactionType.INCOME) {
                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
            } else {
                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
            }
        }
    }
}