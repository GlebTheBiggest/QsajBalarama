package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.controller.TransactionsController;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public class ActionTableCell extends TableCell<Transaction, Void> {

    private final HBox container = new HBox(5);
    private final TransactionsController controller;

    public ActionTableCell(TransactionsController controller) {
        this.controller = controller;
        container.setAlignment(Pos.CENTER);
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
            setGraphic(null);
            return;
        }

        Transaction t = getTableRow().getItem();

        // Системні трансфери не можна редагувати або видаляти
        if (t.getType() == TransactionType.TRANSFER) {
            setGraphic(null);
            return;
        }

        container.getChildren().clear();

        Button editButton = new Button("✎");
        editButton.getStyleClass().add("button-action-edit");
        editButton.setTooltip(new Tooltip("Edit"));
        editButton.setOnAction(e -> controller.startEdit(t));

        Button deleteButton = new Button("✖");
        deleteButton.getStyleClass().add("button-action-delete");
        deleteButton.setTooltip(new Tooltip("Delete"));
        deleteButton.setOnAction(e -> controller.deleteTransaction(t));

        container.getChildren().addAll(editButton, deleteButton);
        setGraphic(container);
    }
}