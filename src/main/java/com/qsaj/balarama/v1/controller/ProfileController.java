package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.AppMode;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.service.AuthService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.service.SavingsService;
import com.qsaj.balarama.v1.session.UserSession;
import com.qsaj.balarama.v1.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

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
    @FXML private Label savingsInGoalsLabel;
    @FXML private Label totalSavingsLabel;   // НОВЕ ПОЛЕ

    private final UserSession userSession;
    private final AuthService authService;
    private final NavigationService navigationService;
    private final SavingsService savingsService;

    @Inject
    public ProfileController(UserSession userSession, AuthService authService,
                             NavigationService navigationService, SavingsService savingsService) {
        this.userSession = userSession;
        this.authService = authService;
        this.navigationService = navigationService;
        this.savingsService = savingsService;
    }

    @FXML
    public void initialize() {
        currencyComboBox.getItems().setAll(Currency.values());
        refreshUserData();
        setupFieldValidation();
    }

    private void setupFieldValidation() {
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty() || newVal.equals(userSession.getCurrentUser().getUsername())) {
                usernameField.getStyleClass().remove("field-modified");
            } else {
                if (!usernameField.getStyleClass().contains("field-modified")) {
                    usernameField.getStyleClass().add("field-modified");
                }
            }
        });

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty() || newVal.equals(userSession.getCurrentUser().getEmail())) {
                emailField.getStyleClass().remove("field-modified");
            } else {
                if (!emailField.getStyleClass().contains("field-modified")) {
                    emailField.getStyleClass().add("field-modified");
                }
            }
        });
    }

    private void refreshUserData() {
        User user = userSession.getCurrentUser();
        headerUsernameLabel.setText(user.getUsername());
        headerEmailLabel.setText(user.getEmail());

        usernameField.setPromptText(user.getUsername());
        usernameField.clear();
        usernameField.getStyleClass().remove("field-modified");

        emailField.setPromptText(user.getEmail());
        emailField.clear();
        emailField.getStyleClass().remove("field-modified");

        Currency currentCurrency = user.getCurrency() != null ? user.getCurrency() : Currency.USD;
        currencyComboBox.setValue(currentCurrency);

        rankLabel.setText(user.getTitle());
        daysActiveLabel.setText(user.getDaysActive() + " days");

        updateSavingsSection(user);
    }

    private void updateSavingsSection(User user) {
        String symbol = user.getCurrency().getSymbol();
        BigDecimal totalInGoals = savingsService.getTotalSavingsInGoals(user);
        BigDecimal unallocated = user.getUnallocatedSavings() != null ? user.getUnallocatedSavings() : BigDecimal.ZERO;
        BigDecimal totalSavings = totalInGoals.add(unallocated);

        if (user.getAppMode() == AppMode.PERIODIC) {
            savingsInGoalsLabel.setText(symbol + " " + totalInGoals);
            savingsInGoalsLabel.setStyle("-fx-text-fill: -color-success;");
            totalSavingsLabel.setText(symbol + " " + totalSavings);
            totalSavingsLabel.setStyle("-fx-text-fill: -color-success;");
        } else {
            savingsInGoalsLabel.setText("Available in Periodic mode");
            savingsInGoalsLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
            totalSavingsLabel.setText("Available in Periodic mode");
            totalSavingsLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
        }
    }

    @FXML
    private void handleUpdateCurrency() {
        Currency selectedCurrency = currencyComboBox.getValue();
        User user = userSession.getCurrentUser();

        if (selectedCurrency == null || selectedCurrency == user.getCurrency()) {
            return;
        }

        try {
            authService.updateCurrency(user, selectedCurrency);
            user.setCurrency(selectedCurrency);
            userSession.setCurrentUser(user);
            refreshUserData();
            navigationService.showAlert("Success", "Currency updated to " + selectedCurrency.name());
        } catch (Exception e) {
            navigationService.showAlert("Error", "Could not update currency.");
        }
    }

    @FXML
    private void handleUpdateUsername() {
        String newUsername = usernameField.getText().trim();
        User user = userSession.getCurrentUser();

        if (newUsername.isEmpty() || newUsername.equals(user.getUsername())) {
            return;
        }

        try {
            authService.updateProfile(user, newUsername, user.getEmail());
            user.setUsername(newUsername);
            userSession.setCurrentUser(user);
            refreshUserData();
            navigationService.showAlert("Success", "Username updated!");
        } catch (IllegalArgumentException e) {
            navigationService.showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateEmail() {
        String newEmail = emailField.getText().trim();
        User user = userSession.getCurrentUser();

        if (newEmail.isEmpty() || newEmail.equals(user.getEmail())) {
            return;
        }

        try {
            authService.updateProfile(user, user.getUsername(), newEmail);
            user.setEmail(newEmail);
            userSession.setCurrentUser(user);
            refreshUserData();
            navigationService.showAlert("Success", "Email updated!");
        } catch (IllegalArgumentException e) {
            navigationService.showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handleChangePassword() {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (!validatePasswordFields(currentPass, newPass, confirmPass)) {
            return;
        }

        try {
            User currentUser = userSession.getCurrentUser();
            authService.changePassword(currentUser, currentPass, newPass);
            navigationService.showAlert("Success", "Password changed successfully!");
            clearPasswordFields();
        } catch (IllegalArgumentException e) {
            navigationService.showAlert("Error", e.getMessage());
        } catch (Exception e) {
            navigationService.showAlert("System Error", "Could not update password.");
        }
    }

    private boolean validatePasswordFields(String currentPass, String newPass, String confirmPass) {
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            navigationService.showAlert("Validation Error", "Please fill in all password fields.");
            return false;
        }

        if (!newPass.equals(confirmPass)) {
            navigationService.showAlert("Error", "New passwords do not match.");
            return false;
        }

        if (!ValidationUtil.isValidPassword(newPass)) {
            navigationService.showAlert("Invalid Password", ValidationUtil.PASSWORD_VALIDATION_MESSAGE);
            return false;
        }

        return true;
    }

    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }
}