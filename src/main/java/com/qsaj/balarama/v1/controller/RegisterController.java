package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.service.AuthService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.util.PasswordStrengthChecker;
import com.qsaj.balarama.v1.util.PasswordVisibilityToggler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;

public class RegisterController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordTextField;
    @FXML
    private ProgressBar passwordStrengthBar;
    @FXML
    private Label passwordStrengthLabel;
    @FXML
    private SVGPath passwordIcon;

    private final AuthService authService;
    private final NavigationService navigationService;
    private PasswordVisibilityToggler passwordToggler;

    @Inject
    public RegisterController(AuthService authService, NavigationService navigationService) {
        this.authService = authService;
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        this.passwordToggler = new PasswordVisibilityToggler(passwordField, passwordTextField, passwordIcon);
        passwordToggler.initialize();
        passwordField.textProperty().addListener((obs, oldText, newText) -> updatePasswordStrength(newText));
        passwordTextField.textProperty().addListener((obs, oldText, newText) -> updatePasswordStrength(newText));
    }

    private void updatePasswordStrength(String password) {
        PasswordStrengthChecker.StrengthResult result = PasswordStrengthChecker.calculateStrength(password);
        passwordStrengthBar.setProgress(result.getProgress());
        passwordStrengthBar.getStyleClass().setAll("progress-bar", result.getStyleClass());
        passwordStrengthLabel.setText(result.getLabel());
        applyStrengthLabelStyle(result.getStyleClass());
    }

    private void applyStrengthLabelStyle(String styleClass) {
        passwordStrengthLabel.getStyleClass().removeAll("label-weak", "label-medium", "label-strong", "label-default");
        switch (styleClass) {
            case "strength-bar-weak" -> passwordStrengthLabel.getStyleClass().add("label-weak");
            case "strength-bar-medium" -> passwordStrengthLabel.getStyleClass().add("label-medium");
            case "strength-bar-strong" -> passwordStrengthLabel.getStyleClass().add("label-strong");
            default -> passwordStrengthLabel.getStyleClass().add("label-default");
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordToggler.toggle();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            navigationService.showAlert("Error", "Please fill in all fields.");
            return;
        }
        if (!password.matches("^[a-zA-Z0-9_]+$")) {
            navigationService.showAlert("Invalid Password", "Letters, numbers and underscores only.");
            return;
        }

        try {
            authService.register(username, email, password);
            navigationService.showAlert("Success", "Account created!");
            navigationService.navigateTo("/fxml/login.fxml", "Qsaj Balarama - Login", 1080, 720);
        } catch (Exception e) {
            navigationService.showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handleLoginLink() {
        navigationService.navigateTo("/fxml/login.fxml", "Qsaj Balarama - Login", 1080, 720);
    }
}