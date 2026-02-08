package com.qsaj.balarama.v1.service;

import javafx.scene.Node;
import java.io.IOException;

public interface NavigationService {
    void navigateTo(String fxmlPath, String title, boolean maximized);
    void navigateTo(String fxmlPath, String title, double width, double height);
    Node loadView(String fxmlPath) throws IOException;
    void showAlert(String title, String content);
}