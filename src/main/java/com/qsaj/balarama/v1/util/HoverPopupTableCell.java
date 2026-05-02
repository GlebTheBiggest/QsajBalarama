package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.model.Transaction;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;

import java.util.Objects;

public abstract class HoverPopupTableCell<T> extends TableCell<Transaction, T> {
    private final Popup popup;
    private final Label popupContent;

    // Зберігаємо повний текст, який має бути в підказці
    private String fullTextToDisplay = null;

    public HoverPopupTableCell() {
        this.popup = new Popup();
        this.popup.setAutoHide(true);

        this.popupContent = new Label();
        this.popupContent.getStyleClass().add("notes-popup-text");
        this.popupContent.setWrapText(true);
        this.popupContent.setMaxWidth(300);

        VBox box = new VBox(popupContent);
        box.getStyleClass().add("notes-popup-box");
        box.getStyleClass().add("root"); // Додаємо root для доступу до CSS-змінних

        try {
            box.getStylesheets().addAll(
                    Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                    Objects.requireNonNull(getClass().getResource("/css/transactions.css")).toExternalForm()
            );
        } catch (Exception ignored) {}

        popup.getContent().add(box);

        // ГОЛОВНЕ ВИПРАВЛЕННЯ: Перевіряємо, чи обрізається текст ТІЛЬКИ в момент наведення мишки.
        // У цей момент графічний інтерфейс повністю сформований, застосовані всі шрифти та відступи.
        setOnMouseEntered((MouseEvent e) -> {
            if (fullTextToDisplay == null || fullTextToDisplay.isEmpty() || getWidth() <= 0) {
                return;
            }

            // 1. Створюємо віртуальний текст, щоб дізнатися його "чисту" ширину в пікселях
            Text textNode = new Text(fullTextToDisplay);
            textNode.setFont(getFont() != null ? getFont() : Font.getDefault());
            double textWidth = textNode.getLayoutBounds().getWidth();

            // 2. Рахуємо, скільки реально місця є під букви (Ширина клітинки мінус CSS-відступи)
            double availableWidth = getWidth() - getPadding().getLeft() - getPadding().getRight();

            // 3. Запасний варіант від самого JavaFX: чи просить клітинка більше місця, ніж має
            double desiredCellWidth = prefWidth(-1);

            // Якщо чистому тексту не вистачає місця, АБО клітинка вимагає ширшої колонки -> показуємо Popup
            if (textWidth > availableWidth || desiredCellWidth > getWidth() + 1) {
                popupContent.setText(fullTextToDisplay);
                popup.show(this, e.getScreenX() + 10, e.getScreenY() + 10);
            }
        });

        // Ховаємо підказку, коли мишка йде з клітинки
        setOnMouseExited(e -> popup.hide());
    }

    // Цей метод тепер просто зберігає текст у пам'ять для перевірки при наведенні
    protected void applyPopupIfTruncated(String displayText) {
        this.fullTextToDisplay = displayText;
        if (displayText == null || displayText.isEmpty()) {
            popup.hide();
        }
    }
}