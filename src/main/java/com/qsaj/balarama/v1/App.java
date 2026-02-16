package com.qsaj.balarama.v1;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.qsaj.balarama.v1.config.AppModule;
import com.qsaj.balarama.v1.service.NavigationService;
import jakarta.persistence.EntityManagerFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    private static final String APP_TITLE = "Qsaj Balarama";
    private static final double MIN_WIDTH = 1000;
    private static final double MIN_HEIGHT = 800;

    private Injector injector;

    @Override
    public void init() {
        this.injector = Guice.createInjector(new AppModule());

        // Ініціалізація БД (переконайтеся, що вона працює)
        try {
            EntityManagerFactory emf = injector.getInstance(EntityManagerFactory.class);
            emf.createEntityManager();
            System.out.println("Database connected successfully!");
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        NavigationService navigationService = injector.getInstance(NavigationService.class);

        // --- ВИПРАВЛЕННЯ ---
        // Ми прибрали 'if (instanceof ...)' і кастинг.
        // Тепер ми викликаємо метод напряму через інтерфейс.
        navigationService.setPrimaryStage(stage);
        // -------------------

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        loader.setControllerFactory(injector::getInstance);

        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 900);

        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

/*
-я от що подумав, я як юзер можу мати якісь свої специфічні потреби і може варто було б додати мені можливість створити свою категорію? тобто я наприклад байкер і мені щодня треба купувати нові шкарпетки і шолом, я міг би створити категорію для байка і все було б простіше і зручніше, але так як у мене зараз категорія це енум то я не знаю як це зробити і чи варто взагалі. може це просто ускладнення яке не потрібне, чи це було б варто зробити?
 */