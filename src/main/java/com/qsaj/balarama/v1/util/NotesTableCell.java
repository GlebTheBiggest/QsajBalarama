package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

public class NotesTableCell extends TableCell<Transaction, String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setTooltip(null);
        } else {
            String display = item.replace("\n", " ");
            if (display.length() > 15) {
                setText(display.substring(0, 15) + "...");
            } else {
                setText(display);
            }
            setTooltip(new Tooltip(item));
        }
    }
}