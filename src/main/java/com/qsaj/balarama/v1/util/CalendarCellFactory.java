package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;

import java.time.LocalDate;
import java.util.List;

public final class CalendarCellFactory {
    private CalendarCellFactory() {}

    public static StackPane create(int day, LocalDate date, List<Transaction> transactions, Popup popup) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("calendar-cell");

        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("calendar-cell-today");
        }

        boolean hasPending = transactions.stream()
                .anyMatch(t -> t.getStatus() == TransactionStatus.PENDING);

        boolean hasCompleted = !transactions.isEmpty() && !hasPending;

        if (hasPending) {
            cell.getStyleClass().add("calendar-cell-pending");
        } else if (hasCompleted) {
            cell.getStyleClass().add("calendar-cell-completed");
        }

        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.getStyleClass().add("calendar-day-label");

        if (hasPending) {
            dayLabel.getStyleClass().add("calendar-day-pending");
        } else if (hasCompleted) {
            dayLabel.getStyleClass().add("calendar-day-completed");
        }

        cell.getChildren().add(dayLabel);

        if (!transactions.isEmpty()) {
            cell.setOnMouseEntered(e -> popup.getContent().setAll(CalendarPopupFactory.create(transactions)));
            cell.setOnMouseExited(e -> popup.hide());
        }

        return cell;
    }
}