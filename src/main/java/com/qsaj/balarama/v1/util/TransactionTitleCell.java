package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

public class TransactionTitleCell extends TableCell<Transaction, String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setTooltip(null);
        } else {
            setText(item);
            Transaction t = getTableRow().getItem();
            if (t != null && t.getNotes() != null && !t.getNotes().isEmpty()) {
                setTooltip(new Tooltip(t.getNotes()));
            }
        }
    }
}