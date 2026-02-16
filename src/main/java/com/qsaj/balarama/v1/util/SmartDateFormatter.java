package com.qsaj.balarama.v1.util;

import javafx.application.Platform;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextFormatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.function.UnaryOperator;

public class SmartDateFormatter {

    private static final String DATE_PATTERN = "dd.MM.yyyy";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_PATTERN)
                    .withResolverStyle(ResolverStyle.STRICT);

    public static void apply(DatePicker datePicker) {
        if (datePicker == null) return;

        datePicker.setEditable(true);

        // Фільтр вводу – дозволяє тільки цифри та крапку, автоматично додає крапки
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (change.isDeleted()) return change;

            // Дозволяємо цифри та крапку
            if (!change.getText().matches("[0-9.]*")) return null;

            // Автоматична крапка при ручному вводі
            if (change.getText().length() == 1 && Character.isDigit(change.getText().charAt(0))) {
                if (newText.length() == 2 || newText.length() == 5) {
                    change.setText(change.getText() + ".");
                    change.setCaretPosition(change.getCaretPosition() + 1);
                    change.setAnchor(change.getAnchor() + 1);
                }
            }

            if (newText.length() > 10) return null;
            return change;
        };

        datePicker.getEditor().setTextFormatter(new TextFormatter<>(filter));

        // Прибираємо червону рамку при змінах
        datePicker.getEditor().textProperty().addListener((o, oldV, newV) -> removeError(datePicker));
        datePicker.valueProperty().addListener((o, oldV, newV) -> removeError(datePicker));

        Platform.runLater(() -> removeError(datePicker));
    }

    private static void removeError(DatePicker picker) {
        picker.getStyleClass().remove("field-error");
        if (picker.getEditor() != null) {
            picker.getEditor().getStyleClass().remove("field-error");
        }
    }

    public static LocalDate getDate(DatePicker picker) {
        if (picker == null) return null;

        // Спочатку перевіряємо значення (якщо вибрано з календаря)
        if (picker.getValue() != null) return picker.getValue();

        // Якщо значення немає, парсимо текст (ручний ввід)
        String text = picker.getEditor() != null ? picker.getEditor().getText() : null;
        if (text != null && text.length() == 10) {
            try {
                return LocalDate.parse(text, DATE_FORMATTER);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static boolean isValid(DatePicker picker) {
        return getDate(picker) != null;
    }
}