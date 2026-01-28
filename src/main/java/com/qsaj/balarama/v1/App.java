package com.qsaj.balarama.v1;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.qsaj.balarama.v1.config.AppModule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private Injector injector;

    @Override
    public void init() {
        this.injector = Guice.createInjector(new AppModule());

        injector.getInstance(jakarta.persistence.EntityManagerFactory.class);
        System.out.println("Database connected successfully!");
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        loader.setControllerFactory(injector::getInstance);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 900);

        // Також додаємо глобальні стилі сюди, якщо ти вибрав цей спосіб (опціонально)
        // scene.getStylesheets().add(getClass().getResource("/css/base.css").toExternalForm());

        // Мінімальний розмір теж трохи збільшимо
        stage.setMinWidth(1000);
        stage.setMinHeight(800);

        stage.setTitle("Qsaj Balarama");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}