package com.qsaj.balarama.v1.util;

public final class ValidationUtil {
    public static final String PASSWORD_VALIDATION_MESSAGE = "Allowed chars: letters, numbers, underscore";
    private static final String PASSWORD_PATTERN = "^[a-zA-Z0-9_]+$";
    private static final String EMAIL_PATTERN = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    private ValidationUtil() {}

    public static void validateEmail(String email) {
        if (email == null || !email.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Invalid email format (e.g. user@mail.com)");
        }
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.matches(PASSWORD_PATTERN);
    }

    public static boolean isFieldModified(String currentValue, String newValue) {
        return newValue != null && !newValue.isEmpty() && !newValue.equals(currentValue);
    }
}