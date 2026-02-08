package com.qsaj.balarama.v1.util;

public class PasswordStrengthChecker {
    private static final String ALLOWED_PASSWORD_PATTERN = "^[a-zA-Z0-9_]+$";

    public static StrengthResult calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new StrengthResult(0, "Enter password", "strength-bar-empty");
        }

        if (!password.matches(ALLOWED_PASSWORD_PATTERN)) {
            return new StrengthResult(0, "Invalid character! Only '_' allowed.", "strength-bar-weak");
        }

        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.contains("_")) score++;

        double progress = (double) score / 6.0;

        if (score < 3) {
            return new StrengthResult(progress, "Weak (use mixed case, numbers and '_')", "strength-bar-weak");
        } else if (score < 5) {
            return new StrengthResult(progress, "Medium", "strength-bar-medium");
        } else {
            return new StrengthResult(progress, "Strong password", "strength-bar-strong");
        }
    }

    public static class StrengthResult {
        private final double progress;
        private final String label;
        private final String styleClass;

        public StrengthResult(double progress, String label, String styleClass) {
            this.progress = progress;
            this.label = label;
            this.styleClass = styleClass;
        }

        public double getProgress() { return progress; }
        public String getLabel() { return label; }
        public String getStyleClass() { return styleClass; }
    }
}