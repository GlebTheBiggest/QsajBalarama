import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.repository.*;
import com.qsaj.balarama.v1.repository.impl.*;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.service.SavingsService;
import com.qsaj.balarama.v1.service.impl.CategoryServiceImpl;
import com.qsaj.balarama.v1.service.impl.EventServiceImpl;
import com.qsaj.balarama.v1.service.impl.FinanceServiceImpl;
import com.qsaj.balarama.v1.service.impl.SavingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsServiceImplTest extends DatabaseTest {

    private SavingsService savingsService;
    private FinanceService financeService;
    private User testUser;

    @BeforeEach
    void setUp() {
        TransactionRepository txRepo = new TransactionRepositoryImpl(() -> em);
        UserRepository userRepo = new UserRepositoryImpl(() -> em);
        SavingsGoalRepository goalRepo = new SavingsGoalRepositoryImpl(() -> em);
        CategoryGroupRepository groupRepo = new CategoryGroupRepositoryImpl(() -> em);
        CategoryItemRepository itemRepo = new CategoryItemRepositoryImpl(() -> em);
        CategoryBudgetRepository budgetRepo = new CategoryBudgetRepositoryImpl(() -> em);
        BudgetPeriodRepository periodRepo = new BudgetPeriodRepositoryImpl(() -> em);

        // Створюємо реальний EventService з репозиторієм
        EventRepository eventRepo = new EventRepositoryImpl(() -> em);
        EventService eventService = new EventServiceImpl(eventRepo);

        CategoryServiceImpl categoryService = new CategoryServiceImpl(groupRepo, itemRepo, () -> em, eventService);

        // Створюємо реальний FinanceService (без бюджету, для спрощення)
        financeService = new FinanceServiceImpl(txRepo, userRepo, null, budgetRepo, goalRepo, groupRepo, itemRepo, eventService);

        savingsService = new SavingsServiceImpl(goalRepo, financeService, categoryService, userRepo, txRepo, eventService);

        executeInTransaction(() -> {
            testUser = User.builder()
                    .username("savingsuser")
                    .email("savings@example.com")
                    .password("hash")
                    .savedAmount(BigDecimal.valueOf(1000))
                    .unallocatedSavings(BigDecimal.valueOf(200)) // Початковий Unallocated = 200
                    .build();
            em.persist(testUser);
        });
    }

    @Test
    void testCreateGoal() {
        savingsService.createGoal(testUser, "Vacation", BigDecimal.valueOf(500));

        List<SavingsGoal> goals = savingsService.getUserGoals(testUser);
        assertThat(goals).hasSize(1);
        SavingsGoal goal = goals.get(0);
        assertThat(goal.getName()).isEqualTo("Vacation");
        assertThat(goal.getTargetAmount()).isEqualByComparingTo("500");
        assertThat(goal.getCurrentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(goal.isCompleted()).isFalse();
    }

    @Test
    void testAddFundsToGoalFromMainBalance() {
        savingsService.createGoal(testUser, "Car", BigDecimal.valueOf(1000));
        SavingsGoal goal = savingsService.getUserGoals(testUser).get(0);

        // Додаємо кошти з головного балансу
        savingsService.addFundsToGoalFromBudget(testUser, goal.getId(), BigDecimal.valueOf(300));

        // Перевіряємо, що сума цілі збільшилась
        assertThat(goal.getCurrentAmount()).isEqualByComparingTo("300");

        // Перевіряємо, що баланс користувача зменшився
        User updated = em.find(User.class, testUser.getId());
        assertThat(updated.getSavedAmount()).isEqualByComparingTo("700");
    }

    @Test
    void testAddFundsToGoalFromUnallocated() {
        savingsService.createGoal(testUser, "Emergency", BigDecimal.valueOf(500));
        SavingsGoal goal = savingsService.getUserGoals(testUser).get(0);

        savingsService.addFundsToGoalFromUnallocated(testUser, goal.getId(), BigDecimal.valueOf(100));

        assertThat(goal.getCurrentAmount()).isEqualByComparingTo("100");
        User updated = em.find(User.class, testUser.getId());
        assertThat(updated.getUnallocatedSavings()).isEqualByComparingTo("100"); // 200 - 100 = 100
    }

    @Test
    void testTransferFromGoalToUnallocated() {
        savingsService.createGoal(testUser, "Gadget", BigDecimal.valueOf(300));
        SavingsGoal goal = savingsService.getUserGoals(testUser).get(0);

        // Спочатку додаємо кошти з unallocated (Unallocated стає 200 - 200 = 0)
        savingsService.addFundsToGoalFromUnallocated(testUser, goal.getId(), BigDecimal.valueOf(200));
        assertThat(goal.getCurrentAmount()).isEqualByComparingTo("200");

        // Тепер переказуємо 50 назад на Unallocated (Unallocated стає 0 + 50 = 50)
        savingsService.transferFromGoalToUnallocated(testUser, goal.getId(), BigDecimal.valueOf(50));

        assertThat(goal.getCurrentAmount()).isEqualByComparingTo("150");
        User updated = em.find(User.class, testUser.getId());
        // ВИПРАВЛЕНО: Очікуємо 50
        assertThat(updated.getUnallocatedSavings()).isEqualByComparingTo("50");
    }

    @Test
    void testTransferBetweenGoals() {
        savingsService.createGoal(testUser, "GoalA", BigDecimal.valueOf(500));
        savingsService.createGoal(testUser, "GoalB", BigDecimal.valueOf(300));
        List<SavingsGoal> goals = savingsService.getUserGoals(testUser);
        SavingsGoal goalA = goals.get(0);
        SavingsGoal goalB = goals.get(1);

        savingsService.addFundsToGoalFromUnallocated(testUser, goalA.getId(), BigDecimal.valueOf(200));

        savingsService.transferBetweenGoals(testUser, goalA.getId(), goalB.getId(), BigDecimal.valueOf(80));

        // Оновлюємо об'єкти з бази
        em.refresh(goalA);
        em.refresh(goalB);

        assertThat(goalA.getCurrentAmount()).isEqualByComparingTo("120");
        assertThat(goalB.getCurrentAmount()).isEqualByComparingTo("80");
    }

    @Test
    void testCompleteGoal() {
        savingsService.createGoal(testUser, "House", BigDecimal.valueOf(200));
        SavingsGoal initialGoal = savingsService.getUserGoals(testUser).get(0);

        // Робимо правильний ТРАНСФЕР з нерозподілених заощаджень
        savingsService.addFundsToGoalFromUnallocated(testUser, initialGoal.getId(), BigDecimal.valueOf(200));

        // Завершуємо ціль
        savingsService.completeGoal(initialGoal.getId(), testUser, "Finally!");

        // Дістаємо свіжий об'єкт
        SavingsGoal updatedGoal = em.find(SavingsGoal.class, initialGoal.getId());

        assertThat(updatedGoal.isCompleted()).isTrue();
        assertThat(updatedGoal.getCompletionNote()).isEqualTo("Finally!");
        assertThat(updatedGoal.getCompletionDate()).isNotNull();

        // Оскільки це був трансфер, а не витрата з бюджету, транзакцій EXPENSE дійсно 0. І це правильно!
        assertThat(updatedGoal.getTotalTransactions()).isEqualTo(0);
    }

    @Test
    void testDeleteGoalWithRefund() {
        savingsService.createGoal(testUser, "RefundGoal", BigDecimal.valueOf(200));
        SavingsGoal goal = savingsService.getUserGoals(testUser).get(0);

        // Додаємо 150 з Unallocated (Unallocated стає 200 - 150 = 50)
        savingsService.addFundsToGoalFromUnallocated(testUser, goal.getId(), BigDecimal.valueOf(150));

        // Видаляємо ціль, 150 повертається назад (Unallocated стає 50 + 150 = 200)
        savingsService.deleteGoal(goal.getId(), testUser);

        List<SavingsGoal> goals = savingsService.getUserGoals(testUser);
        assertThat(goals).isEmpty();

        User updated = em.find(User.class, testUser.getId());
        // ВИПРАВЛЕНО: Очікуємо 200, бо гроші повернулись до початкового стану
        assertThat(updated.getUnallocatedSavings()).isEqualByComparingTo("200");
    }
}