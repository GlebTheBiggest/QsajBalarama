package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.controller.TransactionsController;
import com.qsaj.balarama.v1.model.Transaction;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public class ActionTableCell extends TableCell<Transaction, Void> {

    private final HBox container = new HBox(5);

    public ActionTableCell(TransactionsController controller) {

        Button editButton = new Button("✎");
        editButton.getStyleClass().add("button-action-edit");
        editButton.setTooltip(new Tooltip("Edit"));
        editButton.setOnAction(e -> {
            Transaction t = getTableRow().getItem();
            if (t != null) {
                controller.startEdit(t);
            }
        });

        Button deleteButton = new Button("✖");
        deleteButton.getStyleClass().add("button-action-delete");
        deleteButton.setTooltip(new Tooltip("Delete"));
        deleteButton.setOnAction(e -> {
            Transaction t = getTableRow().getItem();
            if (t != null) {
                controller.deleteTransaction(t);
            }
        });

        container.getChildren().addAll(editButton, deleteButton);
        container.setAlignment(Pos.CENTER);
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : container);
    }
}