import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.repository.*;
import com.qsaj.balarama.v1.repository.impl.*;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.service.impl.FinanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FinanceServiceImplTest extends DatabaseTest {

    private FinanceServiceImpl financeService;
    private User testUser;
    private CategoryGroup testGroup;
    private CategoryItem testItem;

    @Mock
    private BudgetService budgetService;

    @Mock
    private EventService eventService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        TransactionRepository txRepo = new TransactionRepositoryImpl(() -> em);
        UserRepository userRepo = new UserRepositoryImpl(() -> em);
        CategoryBudgetRepository budgetRepo = new CategoryBudgetRepositoryImpl(() -> em);
        SavingsGoalRepository goalRepo = new SavingsGoalRepositoryImpl(() -> em);
        CategoryGroupRepository groupRepo = new CategoryGroupRepositoryImpl(() -> em);
        CategoryItemRepository itemRepo = new CategoryItemRepositoryImpl(() -> em);

        financeService = new FinanceServiceImpl(
                txRepo, userRepo, budgetService, budgetRepo, goalRepo, groupRepo, itemRepo, eventService);

        executeInTransaction(() -> {
            testUser = User.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password("hash")
                    .savedAmount(BigDecimal.valueOf(1000))
                    .unallocatedSavings(BigDecimal.ZERO)
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
        });
    }

    @Test
    void testAddExpenseTransaction() {
        when(budgetService.getActivePeriod(any())).thenReturn(null);

        financeService.addTransaction(
                testUser,
                "Buy groceries",
                BigDecimal.valueOf(50),
                TransactionType.EXPENSE,
                testItem,
                LocalDate.now(),
                "Weekly shopping",
                null,
                TransactionStatus.COMPLETED,
                Priority.STANDARD
        );

        var transactions = financeService.getUserTransactions(testUser);
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo("50");

        User updated = em.find(User.class, testUser.getId());
        assertThat(updated.getSavedAmount()).isEqualByComparingTo("950");
    }

    @Test
    void testAddIncomeTransaction() {
        when(budgetService.getActivePeriod(any())).thenReturn(null);

        financeService.addTransaction(
                testUser,
                "Salary",
                BigDecimal.valueOf(2000),
                TransactionType.INCOME,
                null,
                LocalDate.now(),
                "Monthly salary",
                null,
                TransactionStatus.COMPLETED,
                Priority.ESSENTIAL
        );

        User updated = em.find(User.class, testUser.getId());
        assertThat(updated.getSavedAmount()).isEqualByComparingTo("3000");
    }

    @Test
    void testDeleteTransactionWithRefund() {
        financeService.addTransaction(
                testUser, "Test", BigDecimal.valueOf(100),
                TransactionType.EXPENSE, testItem, LocalDate.now(),
                null, null, TransactionStatus.COMPLETED, Priority.STANDARD);

        var tx = financeService.getUserTransactions(testUser).get(0);
        financeService.deleteTransaction(tx.getId(), testUser);

        var transactions = financeService.getUserTransactions(testUser);
        assertThat(transactions).isEmpty();

        User updated = em.find(User.class, testUser.getId());
        assertThat(updated.getSavedAmount()).isEqualByComparingTo("1000");
    }

    @Test
    void testNegativeBalanceAllowed() {
        when(budgetService.getActivePeriod(any())).thenReturn(null);
        financeService.addTransaction(
                testUser, "Too expensive", BigDecimal.valueOf(2000),
                TransactionType.EXPENSE, testItem, LocalDate.now(),
                null, null, TransactionStatus.COMPLETED, Priority.STANDARD);

        var transactions = financeService.getUserTransactions(testUser);
        assertThat(transactions).hasSize(1);
        User updated = em.find(User.class, testUser.getId());
        assertThat(updated.getSavedAmount()).isEqualByComparingTo("-1000");
    }
}