package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.service.QuoteService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.shape.SVGPath;

public class LoginController {

    private static final String ICON_EYE_OPEN = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";

    private static final String ICON_EYE_CLOSED = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 3.1 0 5.81-1.53 8.18-4.16l2.27 2.27 1.27-1.27L3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Label quoteLabel;

    @FXML private SVGPath passwordIcon;

    private final QuoteService quoteService;

    @Inject
    public LoginController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @FXML
    public void initialize() {
        setupPasswordVisibilitySync();
        quoteLabel.setText(quoteService.getRandomQuote());
    }

    private void setupPasswordVisibilitySync() {
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            if (!passwordTextField.isVisible()) passwordTextField.setText(newText);
        });
        passwordTextField.textProperty().addListener((obs, oldText, newText) -> {
            if (passwordTextField.isVisible()) passwordField.setText(newText);
        });
    }

    @FXML
    private void togglePasswordVisibility() {
        if (passwordTextField.isVisible()) {
            passwordTextField.setVisible(false);
            passwordField.setVisible(true);
            passwordIcon.setContent(ICON_EYE_OPEN);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        } else {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordField.setVisible(false);
            passwordIcon.setContent(ICON_EYE_CLOSED);
            passwordTextField.requestFocus();
            passwordTextField.positionCaret(passwordTextField.getText().length());
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }
        System.out.println("Login attempt: " + username);
    }

    @FXML
    private void handleForgotPassword() {
        showAlert("Info", "Password recovery feature coming soon.");
    }

    @FXML
    private void handleForgotUsername() {
        showAlert("Info", "Username recovery feature coming soon.");
    }

    @FXML
    private void handleRegister() {
        System.out.println("Navigating to Register...");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}