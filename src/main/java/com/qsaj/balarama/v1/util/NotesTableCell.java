package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;

import java.util.Objects;

public class NotesTableCell extends TableCell<Transaction, String> {
    private final Popup popup;
    private final Label popupContent;

    public NotesTableCell() {
        this.popup = new Popup();
        this.popup.setAutoHide(true);

        this.popupContent = new Label();
        this.popupContent.getStyleClass().add("notes-popup-text");
        this.popupContent.setWrapText(true);
        this.popupContent.setMaxWidth(300);

        VBox box = new VBox(popupContent);
        box.getStyleClass().add("notes-popup-box");
        box.getStyleClass().add("root"); // Додаємо root для доступу до CSS-змінних

        // Підключаємо обидва CSS: base.css (для змінних) та transactions.css (для класів)
        box.getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/transactions.css")).toExternalForm()
        );

        popup.getContent().add(box);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null || item.isEmpty()) {
            setText(null);
            setGraphic(null);
            setOnMouseEntered(null);
            setOnMouseExited(null);
            return;
        }

        setText(item);

        Platform.runLater(() -> {
            try {
                Text textNode = new Text(item);
                textNode.setFont(Font.font(getFont().getFamily(), getFont().getSize()));
                Bounds textBounds = textNode.getBoundsInLocal();
                double textWidth = textBounds.getWidth();
                double cellWidth = getWidth() - 20;

                if (textWidth > cellWidth) {
                    setOnMouseEntered((MouseEvent e) -> showPopup(e, item));
                    setOnMouseExited(e -> popup.hide());
                } else {
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                    popup.hide();
                }
            } catch (Exception ex) {
                setOnMouseEntered(null);
                setOnMouseExited(null);
            }
        });
    }

    private void showPopup(MouseEvent e, String fullText) {
        popupContent.setText(fullText);
        popup.show(this, e.getScreenX() + 10, e.getScreenY() + 10);
    }
}