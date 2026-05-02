package com.qsaj.balarama.v1.util;

public class StatusTableCell extends HoverPopupTableCell<String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().removeAll("status-pending", "status-completed", "status-overdue");

        if (empty || item == null) {
            setText(null);
            applyPopupIfTruncated(null);
        } else {
            setText(item);
            switch (item) {
                case "PENDING" -> getStyleClass().add("status-pending");
                case "COMPLETED" -> getStyleClass().add("status-completed");
                default -> getStyleClass().add("status-overdue");
            }
            applyPopupIfTruncated(item); // Викликаємо попап
        }
    }
}