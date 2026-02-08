package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public final class CalendarPopupFactory {
    private CalendarPopupFactory() {}

    public static VBox create(List<Transaction> transactions) {
        VBox box = new VBox(5);
        box.getStyleClass().add("calendar-popup");

        Label header = new Label("Events");
        header.getStyleClass().add("calendar-popup-header");
        box.getChildren().add(header);

        transactions.forEach(t -> {
            Label label = new Label(buildText(t));
            label.getStyleClass().add(resolveStyle(t));
            box.getChildren().add(label);
        });

        return box;
    }

    private static String buildText(Transaction t) {
        String marker = t.getStatus() == TransactionStatus.PENDING ? "⏳" : "✔";
        return String.format("%s %s • %s", marker, t.getTitle(), t.getAmount());
    }

    private static String resolveStyle(Transaction t) {
        if (t.getStatus() == TransactionStatus.PENDING) return "popup-pending";
        return t.getType() == TransactionType.EXPENSE ? "popup-expense" : "popup-income";
    }
}