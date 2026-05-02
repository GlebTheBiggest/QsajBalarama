package com.qsaj.balarama.v1.util;

public class TextPopupCell extends HoverPopupTableCell<String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isEmpty()) {
            setText(null);
            applyPopupIfTruncated(null);
        } else {
            setText(item);
            applyPopupIfTruncated(item);
        }
    }
}