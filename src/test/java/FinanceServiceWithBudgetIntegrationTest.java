import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.repository.*;
import com.qsaj.balarama.v1.repository.impl.*;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.service.impl.BudgetServiceImpl;
import com.qsaj.balarama.v1.service.impl.CategoryServiceImpl;
import com.qsaj.balarama.v1.service.impl.EventServiceImpl;
import com.qsaj.balarama.v1.service.impl.FinanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinanceServiceWithBudgetIntegrationTest extends DatabaseTest {

    private FinanceService financeService;
    private BudgetService budgetService;
    private User testUser;
    private CategoryGroup testGroup;
    private CategoryItem testItem;

    @Mock
    private NavigationService navigationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        TransactionRepository txRepo = new TransactionRepositoryImpl(() -> em);
        UserRepository userRepo = new UserRepositoryImpl(() -> em);
        CategoryBudgetRepository budgetRepo = new CategoryBudgetRepositoryImpl(() -> em);
        SavingsGoalRepository goalRepo = new SavingsGoalRepositoryImpl(() -> em);
        CategoryGroupRepository groupRepo = new CategoryGroupRepositoryImpl(() -> em);
        CategoryItemRepository itemRepo = new CategoryItemRepositoryImpl(() -> em);
        BudgetPeriodRepository periodRepo = new BudgetPeriodRepositoryImpl(() -> em);

        // Створюємо реальний EventService
        EventRepository eventRepo = new EventRepositoryImpl(() -> em);
        EventService eventService = new EventServiceImpl(eventRepo);

        CategoryServiceImpl categoryService = new CategoryServiceImpl(groupRepo, itemRepo, () -> em, eventService);

        // Створюємо реальний BudgetService з моком NavigationService
        budgetService = new BudgetServiceImpl(periodRepo, budgetRepo, userRepo, null, categoryService, txRepo, navigationService, eventService);

        // Створюємо реальний FinanceService з бюджетом
        financeService = new FinanceServiceImpl(txRepo, userRepo, budgetService, budgetRepo, goalRepo, groupRepo, itemRepo, eventService);

        executeInTransaction(() -> {
            testUser = User.builder()
                    .username("intuser")
                    .email("int@example.com")
                    .password("hash")
                    .savedAmount(BigDecimal.valueOf(1000))
                    .unallocatedSavings(BigDecimal.ZERO)
                    .appMode(AppMode.PERIODIC)
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

            // Створюємо активний період
            BudgetPeriod period = BudgetPeriod.builder()
                    .user(testUser)
                    .startDate(LocalDate.now().withDayOfMonth(1))
                    .endDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
                    .active(true)
                    .build();
            em.persist(period);

            // Встановлюємо бюджет на категорію Food = 300
            CategoryBudget budget = CategoryBudget.builder()
                    .budgetPeriod(period)
                    .categoryGroup(testGroup)
                    .allocatedAmount(BigDecimal.valueOf(300))
                    .spentAmount(BigDecimal.ZERO)
                    .build();
            em.persist(budget);
        });
    }

    @Test
    void testAddExpenseWithinBudget() {
        financeService.addTransaction(testUser, "Buy food", BigDecimal.valueOf(200), TransactionType.EXPENSE,
                testItem, LocalDate.now(), null, null, TransactionStatus.COMPLETED, Priority.STANDARD);

        // Перевіряємо, що витрати оновились у бюджеті
        BudgetPeriod active = budgetService.getActivePeriod(testUser);
        List<CategoryBudget> budgets = budgetService.getBudgetsForPeriod(active);
        CategoryBudget foodBudget = budgets.stream().filter(b -> b.getCategoryGroup().getId().equals(testGroup.getId())).findFirst().get();
        assertThat(foodBudget.getSpentAmount()).isEqualByComparingTo("200");

        // Баланс користувача зменшився
        User updated = em.find(User.class, testUser.getId());
        assertThat(updated.getSavedAmount()).isEqualByComparingTo("800");
    }

    @Test
    void testAddExpenseExceedingBudget() {
        assertThatThrownBy(() -> {
            financeService.addTransaction(testUser, "Expensive food", BigDecimal.valueOf(400), TransactionType.EXPENSE,
                    testItem, LocalDate.now(), null, null, TransactionStatus.COMPLETED, Priority.STANDARD);
        }).hasMessageContaining("Exceeds budget limit");
    }
}