package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import javafx.scene.control.TableCell;

public class StatusTableCell extends TableCell<Transaction, String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setStyle("");
        } else {
            setText(item);
            if ("PENDING".equals(item)) {
                setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
            } else if ("COMPLETED".equals(item)) {
                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            }
        }
    }
}