package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import javafx.scene.control.TableCell;

public class StatusTableCell extends TableCell<Transaction, String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().removeAll("status-pending", "status-completed", "status-overdue");

        if (empty || item == null) {
            setText(null);
        } else {
            setText(item);
            switch (item) {
                case "PENDING" -> getStyleClass().add("status-pending");
                case "COMPLETED" -> getStyleClass().add("status-completed");
                default -> getStyleClass().add("status-overdue");
            }
        }
    }
}