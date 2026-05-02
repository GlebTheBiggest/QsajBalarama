import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import com.qsaj.balarama.v1.repository.impl.TransactionRepositoryImpl;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.MLService;
import com.qsaj.balarama.v1.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AnalyticsServiceImplTest extends DatabaseTest {

    private MLService mlService;
    private User testUser;
    private CategoryGroup testGroup;
    private CategoryItem testItem;

    // Створюємо мок для нового сервісу, від якого тепер залежить MLServiceImpl
    @Mock
    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        // Ініціалізуємо моки
        MockitoAnnotations.openMocks(this);

        TransactionRepository txRepo = new TransactionRepositoryImpl(() -> em);

        // Тепер передаємо ОБИДВА параметри
        mlService = new AnalyticsServiceImpl(txRepo, budgetService);

        executeInTransaction(() -> {
            testUser = User.builder()
                    .username("mluser")
                    .email("ml@example.com")
                    .password("hash")
                    .currency(Currency.USD)
                    .build();
            em.persist(testUser);

            testGroup = CategoryGroup.builder()
                    .name("Food")
                    .type(TransactionType.EXPENSE)
                    .color("#FF5733")
                    .user(testUser)
                    .build();
            em.persist(testGroup);

            testItem = CategoryItem.builder()
                    .name("Groceries")
                    .group(testGroup)
                    .build();
            em.persist(testItem);

            // Додаємо тестові транзакції за останні 6 місяців
            LocalDate now = LocalDate.now();
            for (int i = 0; i < 6; i++) {
                Transaction tx = Transaction.builder()
                        .title("Test expense " + i)
                        .amount(BigDecimal.valueOf(100 + i * 10))
                        .type(TransactionType.EXPENSE)
                        .categoryItem(testItem)
                        .date(now.minusMonths(i))
                        .user(testUser)
                        .status(TransactionStatus.COMPLETED)
                        .priority(Priority.STANDARD)
                        .build();
                em.persist(tx);
            }
        });
    }

    @Test
    void testGenerateInsightsWithData() {
        // Налаштовуємо мок, щоб він повертав null (ми не тестуємо бюджети в цьому конкретному тесті)
        when(budgetService.getActivePeriod(any(User.class))).thenReturn(null);

        LocalDate start = LocalDate.now().minusMonths(6);
        LocalDate end = LocalDate.now();
        List<MLInsight> insights = mlService.generateInsights(testUser, start, end);

        assertThat(insights).isNotEmpty();
        // Перевіряємо, що є різні типи інсайтів
        boolean hasSpendingPattern = insights.stream().anyMatch(i -> i.getType() == MLInsight.InsightType.SPENDING_PATTERN);
        boolean hasForecast = insights.stream().anyMatch(i -> i.getType() == MLInsight.InsightType.FORECAST);

        assertThat(hasSpendingPattern).isTrue();
        assertThat(hasForecast).isTrue();
    }

    @Test
    void testGenerateInsightsEmpty() {
        LocalDate start = LocalDate.now().plusMonths(1);
        LocalDate end = LocalDate.now().plusMonths(2);
        List<MLInsight> insights = mlService.generateInsights(testUser, start, end);

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).getTitle()).isEqualTo("No Data");
    }
}