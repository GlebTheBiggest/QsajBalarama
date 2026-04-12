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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BudgetServiceImplTest extends DatabaseTest {

    private BudgetService budgetService;
    private User testUser;
    private CategoryGroup testGroup;

    @Mock
    private FinanceService financeService;
    @Mock
    private NavigationService navigationService;
    @Mock
    private EventService eventService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        BudgetPeriodRepository periodRepo = new BudgetPeriodRepositoryImpl(() -> em);
        CategoryBudgetRepository budgetRepo = new CategoryBudgetRepositoryImpl(() -> em);
        UserRepository userRepo = new UserRepositoryImpl(() -> em);
        TransactionRepository txRepo = new TransactionRepositoryImpl(() -> em);
        CategoryGroupRepository groupRepo = new CategoryGroupRepositoryImpl(() -> em);
        CategoryItemRepository itemRepo = new CategoryItemRepositoryImpl(() -> em);

        CategoryServiceImpl categoryService = new CategoryServiceImpl(groupRepo, itemRepo, () -> em, eventService);

        budgetService = new BudgetServiceImpl(periodRepo, budgetRepo, userRepo, financeService,
                categoryService, txRepo, navigationService, eventService);

        executeInTransaction(() -> {
            testUser = User.builder()
                    .username("budgetuser")
                    .email("budget@example.com")
                    .password("hash")
                    .savedAmount(BigDecimal.valueOf(500))
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
        });
    }

    @Test
    void testStartNewPeriod() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);

        BudgetPeriod period = budgetService.startNewPeriod(testUser, start, end);

        assertThat(period).isNotNull();
        assertThat(period.getStartDate()).isEqualTo(start);
        assertThat(period.getEndDate()).isEqualTo(end);
        assertThat(period.isActive()).isTrue();
        assertThat(period.getUser().getId()).isEqualTo(testUser.getId());

        BudgetPeriod active = budgetService.getActivePeriod(testUser);
        assertThat(active.getId()).isEqualTo(period.getId());
    }

    @Test
    void testSetAndGetCategoryBudget() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        BudgetPeriod period = budgetService.startNewPeriod(testUser, start, end);

        BigDecimal limit = BigDecimal.valueOf(300);
        budgetService.setCategoryBudget(period, testGroup, limit);

        List<CategoryBudget> budgets = budgetService.getBudgetsForPeriod(period);
        assertThat(budgets).hasSize(1);
        assertThat(budgets.get(0).getAllocatedAmount()).isEqualByComparingTo(limit);
        assertThat(budgets.get(0).getSpentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(budgets.get(0).getCategoryGroup().getId()).isEqualTo(testGroup.getId());
    }

    @Test
    void testUpdateBudgetLimit() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        BudgetPeriod period = budgetService.startNewPeriod(testUser, start, end);

        budgetService.setCategoryBudget(period, testGroup, BigDecimal.valueOf(200));
        budgetService.setCategoryBudget(period, testGroup, BigDecimal.valueOf(350));

        List<CategoryBudget> budgets = budgetService.getBudgetsForPeriod(period);
        assertThat(budgets.get(0).getAllocatedAmount()).isEqualByComparingTo("350");
    }

    @Test
    void testCannotSetBudgetLessThanSpent() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        BudgetPeriod period = budgetService.startNewPeriod(testUser, start, end);

        // Спочатку встановлюємо ліміт 200
        budgetService.setCategoryBudget(period, testGroup, BigDecimal.valueOf(200));

        // Імітуємо витрати (через пряме оновлення spentAmount)
        CategoryBudget budget = budgetService.getBudgetsForPeriod(period).get(0);
        budget.setSpentAmount(BigDecimal.valueOf(150));
        // (потрібно оновити через репозиторій, але для простоти)

        assertThatThrownBy(() -> {
            budgetService.setCategoryBudget(period, testGroup, BigDecimal.valueOf(100));
        }).hasMessageContaining("cannot be less than already spent amount");
    }

    @Test
    void testCloseActivePeriod() {
        when(navigationService.showConfirmation(any(), any(), any())).thenReturn(true);

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        BudgetPeriod period = budgetService.startNewPeriod(testUser, start, end);

        // Додаємо транзакцію витрати, щоб зменшити баланс
        testUser.setSavedAmount(BigDecimal.valueOf(300));
        executeInTransaction(() -> em.merge(testUser));

        budgetService.closeActivePeriod(testUser);

        // Період має бути неактивним
        BudgetPeriod active = budgetService.getActivePeriod(testUser);
        assertThat(active).isNull();

        // Користувач повинен мати savedAmount = 0, а unallocated збільшився
        User updated = em.find(User.class, testUser.getId());
        assertThat(updated.getSavedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updated.getUnallocatedSavings()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }
}