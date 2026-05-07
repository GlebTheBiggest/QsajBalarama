package com.qsaj.balarama.v1.util;

import javafx.scene.control.DatePicker;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.function.UnaryOperator;

public class SmartDateFormatter {

    // ВАЖЛИВО: У режимі STRICT обов'язково використовувати 'uuuu' замість 'yyyy'
    private static final String DATE_PATTERN = "dd.MM.uuuu";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_PATTERN)
                    .withResolverStyle(ResolverStyle.STRICT);

    public static void apply(DatePicker datePicker) {
        if (datePicker == null) return;

        datePicker.setEditable(true);
        datePicker.setPromptText("DD.MM.YYYY");

        // --- 1. Текстовий фільтр (TextFormatter) ---
        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (change.isDeleted()) return change;

            String newText = change.getControlNewText();

            // Дозволяємо лише правильну структуру: цифри та крапки (напр. "2", "20.", "20.03.")
            if (!newText.matches("^\\d{0,2}(\\.\\d{0,2}(\\.\\d{0,4})?)?$")) {
                return null; // Блокуємо невалідний ввід
            }

            // Автоматичне додавання крапки під час набору
            // Додаємо крапку, якщо ввели день (2 цифри) або місяць (5 символів)
            if ((newText.length() == 2 && !newText.contains(".")) ||
                    (newText.length() == 5 && newText.indexOf('.') == newText.lastIndexOf('.'))) {

                // Просто дописуємо крапку до поточного символу, не перезаписуючи весь рядок
                change.setText(change.getText() + ".");
                change.setCaretPosition(change.getCaretPosition() + 1);
                change.setAnchor(change.getAnchor() + 1);
            }

            return change;
        };

        datePicker.getEditor().setTextFormatter(new TextFormatter<>(filter));

        // --- 2. Конвертер (String <-> LocalDate) ---
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? DATE_FORMATTER.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                try {
                    return LocalDate.parse(string, DATE_FORMATTER);
                } catch (Exception e) {
                    return null;
                }
            }
        });

        // --- 3. Логіка втрати фокусу (Відкат або Збереження) ---
        datePicker.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String text = datePicker.getEditor().getText();

                // Якщо поле пусте - очищаємо
                if (text == null || text.trim().isEmpty()) {
                    datePicker.setValue(null);
                    removeError(datePicker);
                    return;
                }

                try {
                    // Пробуємо розпарсити введену дату
                    LocalDate parsed = LocalDate.parse(text, DATE_FORMATTER);
                    datePicker.setValue(parsed);
                    // Оновлюємо текст (на випадок якщо ввели, наприклад, 01.01.2026 вручну)
                    datePicker.getEditor().setText(DATE_FORMATTER.format(parsed));
                } catch (Exception e) {
                    // Якщо дата введена неправильно - відкочуємось до останньої правильної (або очищаємо)
                    if (datePicker.getValue() != null) {
                        datePicker.getEditor().setText(DATE_FORMATTER.format(datePicker.getValue()));
                    } else {
                        datePicker.getEditor().clear();
                    }
                }
                removeError(datePicker);
            }
        });

        // Прибирання червоної рамки під час виправлення тексту
        datePicker.getEditor().textProperty().addListener((obs, old, newVal) -> removeError(datePicker));
    }

    private static void removeError(DatePicker picker) {
        picker.getStyleClass().remove("field-error");
        if (picker.getEditor() != null) {
            picker.getEditor().getStyleClass().remove("field-error");
        }
    }

    public static LocalDate getDate(DatePicker picker) {
        if (picker == null) return null;
        if (picker.getValue() != null) return picker.getValue();
        String text = picker.getEditor().getText();
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(text, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isValid(DatePicker picker) {
        String text = picker.getEditor().getText();
        if (text == null || text.trim().isEmpty()) return false;
        try {
            LocalDate.parse(text, DATE_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}