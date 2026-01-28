package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.service.AuthService;
import com.qsaj.balarama.v1.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

public class ProfileController {

    @FXML private Label headerUsernameLabel;
    @FXML private Label headerEmailLabel;

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<Currency> currencyComboBox;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label rankLabel;
    @FXML private Label daysActiveLabel;

    // ПРИБРАНО savedAmountLabel, бо в FXML це тепер статичний текст

    private final UserSession userSession;
    private final AuthService authService;

    private static final String ALLOWED_PASSWORD_PATTERN = "^[a-zA-Z0-9_]+$";
    private static final int MAX_USERNAME_LENGTH = 8;

    @Inject
    public ProfileController(UserSession userSession, AuthService authService) {
        this.userSession = userSession;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        currencyComboBox.getItems().setAll(Currency.values());
        addTextLimiter(usernameField, MAX_USERNAME_LENGTH);
        refreshUserData();
    }

    private void refreshUserData() {
        User user = userSession.getCurrentUser();

        headerUsernameLabel.setText(user.getUsername());
        headerEmailLabel.setText(user.getEmail());

        usernameField.setText("");
        emailField.setText("");
        usernameField.setPromptText(user.getUsername());
        emailField.setPromptText(user.getEmail());

        Currency currentCurrency = user.getCurrency() != null ? user.getCurrency() : Currency.USD;
        currencyComboBox.setValue(currentCurrency);

        rankLabel.setText(user.getTitle());
        daysActiveLabel.setText(user.getDaysActive() + " days");

        // ПРИБРАНО логіку встановлення savedAmountLabel
    }

    // ... решта методів (addTextLimiter, handleUpdateUsername і т.д.) залишаються без змін ...

    private void addTextLimiter(final TextField tf, final int maxLength) {
        tf.textProperty().addListener((ov, oldValue, newValue) -> {
            if (tf.getText().length() > maxLength) {
                String s = tf.getText().substring(0, maxLength);
                tf.setText(s);
            }
        });
    }

    @FXML
    private void handleUpdateUsername() {
        String newUsername = usernameField.getText().trim();
        if (newUsername.isEmpty()) return;

        User user = userSession.getCurrentUser();
        if (newUsername.equals(user.getUsername())) return;

        try {
            authService.updateProfile(user, newUsername, user.getEmail());
            userSession.setCurrentUser(user);
            refreshUserData();
            showAlert(AlertType.INFORMATION, "Success", "Username updated!");
        } catch (IllegalArgumentException e) {
            showAlert(AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateEmail() {
        String newEmail = emailField.getText().trim();
        if (newEmail.isEmpty()) return;

        User user = userSession.getCurrentUser();
        if (newEmail.equals(user.getEmail())) return;

        try {
            authService.updateProfile(user, user.getUsername(), newEmail);
            userSession.setCurrentUser(user);
            refreshUserData();
            showAlert(AlertType.INFORMATION, "Success", "Email updated!");
        } catch (IllegalArgumentException e) {
            showAlert(AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateCurrency() {
        Currency selectedCurrency = currencyComboBox.getValue();
        User user = userSession.getCurrentUser();

        if (selectedCurrency == null || selectedCurrency == user.getCurrency()) return;

        try {
            authService.updateCurrency(user, selectedCurrency);
            userSession.setCurrentUser(user);
            refreshUserData();
            showAlert(AlertType.INFORMATION, "Success", "Currency preference updated to " + selectedCurrency.name());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Could not update currency.");
        }
    }

    @FXML
    private void handleChangePassword() {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please fill in all password fields.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showAlert(AlertType.ERROR, "Error", "New passwords do not match.");
            return;
        }
        if (!newPass.matches(ALLOWED_PASSWORD_PATTERN)) {
            showAlert(AlertType.ERROR, "Invalid Password", "Allowed chars: letters, numbers, _");
            return;
        }

        try {
            User currentUser = userSession.getCurrentUser();
            authService.changePassword(currentUser, currentPass, newPass);
            showAlert(AlertType.INFORMATION, "Success", "Password changed successfully!");
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (IllegalArgumentException e) {
            showAlert(AlertType.ERROR, "Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "System Error", "Could not update password.");
        }
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}