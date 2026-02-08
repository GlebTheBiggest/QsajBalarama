package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

import java.util.function.Consumer;

public class ActionTableCell extends TableCell<Transaction, Void> {
    private final Button deleteButton;
    private final Consumer<Transaction> deleteHandler;

    public ActionTableCell(Consumer<Transaction> deleteHandler) {
        this.deleteHandler = deleteHandler;
        this.deleteButton = new Button("X");
        this.deleteButton.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        this.deleteButton.setOnAction(event -> {
            Transaction t = getTableView().getItems().get(getIndex());
            deleteHandler.accept(t);
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(deleteButton);
        }
    }
}