package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.qsaj.balarama.v1.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    private static final String ICON_EYE_OPEN = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String ICON_EYE_CLOSED = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 3.1 0 5.81-1.53 8.18-4.16l2.27 2.27 1.27-1.27L3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";
    private static final String ALLOWED_PASSWORD_PATTERN = "^[a-zA-Z0-9_]+$";

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;
    @FXML private SVGPath passwordIcon;

    private final Injector injector;
    private final AuthService authService;

    @Inject
    public RegisterController(Injector injector, AuthService authService) {
        this.injector = injector;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        setupPasswordVisibilitySync();
        passwordField.textProperty().addListener((obs, oldText, newText) -> updatePasswordStrength(newText));
        passwordTextField.textProperty().addListener((obs, oldText, newText) -> updatePasswordStrength(newText));
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthBar.setProgress(0);
            passwordStrengthBar.getStyleClass().setAll("progress-bar", "strength-bar-empty");
            passwordStrengthLabel.setText("Enter password");
            return;
        }

        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.contains("_")) score++;

        double progress = (double) score / 6.0;
        passwordStrengthBar.setProgress(progress);

        passwordStrengthBar.getStyleClass().clear();
        passwordStrengthBar.getStyleClass().add("progress-bar");

        if (!password.matches(ALLOWED_PASSWORD_PATTERN)) {
            passwordStrengthBar.getStyleClass().add("strength-bar-weak");
            passwordStrengthLabel.setText("Invalid character! Only '_' allowed.");
            passwordStrengthLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px; -fx-font-weight: bold;");
            return;
        }

        if (score < 3) {
            passwordStrengthBar.getStyleClass().add("strength-bar-weak");
            passwordStrengthLabel.setText("Weak (use mixed case, numbers and '_')");
            passwordStrengthLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;");
        } else if (score < 5) {
            passwordStrengthBar.getStyleClass().add("strength-bar-medium");
            passwordStrengthLabel.setText("Medium");
            passwordStrengthLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 10px;");
        } else {
            passwordStrengthBar.getStyleClass().add("strength-bar-strong");
            passwordStrengthLabel.setText("Strong password");
            passwordStrengthLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 10px;");
        }
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
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        if (!password.matches(ALLOWED_PASSWORD_PATTERN)) {
            showAlert("Invalid Password", "Password can only contain letters, numbers, and underscores (_).\nOther special characters are not allowed.");
            return;
        }

        try {
            authService.register(username, email, password);
            showAlert("Success", "Account created! Please log in.");
            goToLogin(null);
        } catch (IllegalArgumentException e) {
            showAlert("Registration Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("System Error", "Registration failed. Check console.");
        }
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        goToLogin(event);
    }

    private void goToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(injector::getInstance);
            Parent root = loader.load();
            Stage stage;
            if (event != null && event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                stage = (Stage) usernameField.getScene().getWindow();
            }
            stage.setScene(new Scene(root, 1080, 720));
            stage.setTitle("Qsaj Balarama - Login");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load Login view.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}