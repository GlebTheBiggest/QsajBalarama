package com.qsaj.balarama.v1.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardController {

    // Ці поля зв'язані з FXML через fx:id
    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Label expenseLabel;
    @FXML private Label incomeLabel;

    // Сюди ми будемо інжектити картки транзакцій
    @FXML private VBox recentTransactionsContainer;

    @FXML
    public void initialize() {
        // Тут поки пусто.
        // Коли ти зробиш TransactionService, ми викличемо тут метод:
        // loadDashboardData();
    }

    /* * Цей метод ти розкоментуєш і допишеш, коли у тебе буде Entity "Transaction".
     * Він прийматиме список і сам будуватиме UI.

    public void updateTransactionsList(List<Transaction> transactions) {
        recentTransactionsContainer.getChildren().clear();
        for (Transaction t : transactions) {
            // Тут буде логіка створення картки:
            // Node card = TransactionCardFactory.create(t);
            // recentTransactionsContainer.getChildren().add(card);
        }
    }
    */
}