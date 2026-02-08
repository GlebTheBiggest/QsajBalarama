package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.service.AuthService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.service.QuoteService;
import com.qsaj.balarama.v1.session.UserSession;
import com.qsaj.balarama.v1.util.PasswordVisibilityToggler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Label quoteLabel;
    @FXML private SVGPath passwordIcon;

    private final QuoteService quoteService;
    private final AuthService authService;
    private final UserSession userSession;
    private final NavigationService navigationService;
    private PasswordVisibilityToggler passwordToggler;

    @Inject
    public LoginController(QuoteService quoteService, AuthService authService,
                           UserSession userSession, NavigationService navigationService) {
        this.quoteService = quoteService;
        this.authService = authService;
        this.userSession = userSession;
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        this.passwordToggler = new PasswordVisibilityToggler(passwordField, passwordTextField, passwordIcon);
        passwordToggler.initialize();
        quoteLabel.setText(quoteService.getRandomQuote());
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordToggler.toggle();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            navigationService.showAlert("Error", "Please fill in all fields.");
            return;
        }

        try {
            User user = authService.authenticate(username, password);
            if (user != null) {
                userSession.setCurrentUser(user);
                System.out.println("Login successful for: " + user.getUsername());
                navigationService.navigateTo("/fxml/main_layout.fxml", "Qsaj Balarama - Dashboard", true);
            } else {
                navigationService.showAlert("Login Failed", "Invalid username or password.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            navigationService.showAlert("System Error", "Database connection failed.");
        }
    }

    @FXML
    private void handleForgotPassword() {
        navigationService.showAlert("Info", "Password recovery feature coming soon.");
    }

    @FXML
    private void handleForgotUsername() {
        navigationService.showAlert("Info", "Username recovery feature coming soon.");
    }

    @FXML
    private void handleRegister() {
        navigationService.navigateTo("/fxml/register.fxml", "Qsaj Balarama - Register", 1080, 720);
    }
}