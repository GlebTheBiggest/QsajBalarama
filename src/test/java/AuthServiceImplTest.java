
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.repository.CategoryGroupRepository;
import com.qsaj.balarama.v1.repository.CategoryItemRepository;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.repository.impl.CategoryGroupRepositoryImpl;
import com.qsaj.balarama.v1.repository.impl.CategoryItemRepositoryImpl;
import com.qsaj.balarama.v1.repository.impl.UserRepositoryImpl;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceImplTest extends DatabaseTest {

    private AuthServiceImpl authService;
    private UserRepository userRepository;

    @Mock
    private EventService eventService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userRepository = new UserRepositoryImpl(() -> em);
        CategoryGroupRepository groupRepo = new CategoryGroupRepositoryImpl(() -> em);
        CategoryItemRepository itemRepo = new CategoryItemRepositoryImpl(() -> em);
        authService = new AuthServiceImpl(userRepository, groupRepo, itemRepo, eventService);
    }

    @Test
    void testRegisterAndAuthenticate() {
        executeInTransaction(() -> {
            authService.register("testuser", "test@example.com", "password123");
        });

        User authenticated = authService.authenticate("testuser", "password123");
        assertThat(authenticated).isNotNull();
        assertThat(authenticated.getUsername()).isEqualTo("testuser");
        assertThat(authenticated.getEmail()).isEqualTo("test@example.com");
        assertThat(authenticated.getSavedAmount()).isEqualByComparingTo("0");
        assertThat(authenticated.getCurrency()).isEqualTo(Currency.USD);
    }

    @Test
    void testRegisterDuplicateUsername() {
        executeInTransaction(() -> {
            authService.register("testuser", "test1@example.com", "pass");
        });

        assertThatThrownBy(() -> {
            executeInTransaction(() -> {
                authService.register("testuser", "test2@example.com", "pass");
            });
        }).hasMessageContaining("Username is already taken");
    }

    @Test
    void testChangePassword() {
        executeInTransaction(() -> {
            authService.register("testuser", "test@example.com", "oldpass");
        });

        User user = authService.authenticate("testuser", "oldpass");
        assertThat(user).isNotNull();

        executeInTransaction(() -> {
            authService.changePassword(user, "oldpass", "newpass123");
        });

        assertThat(authService.authenticate("testuser", "oldpass")).isNull();
        assertThat(authService.authenticate("testuser", "newpass123")).isNotNull();
    }

    @Test
    void testUpdateCurrency() {
        executeInTransaction(() -> {
            authService.register("testuser", "test@example.com", "pass");
        });

        User user = authService.authenticate("testuser", "pass");
        assertThat(user.getCurrency()).isEqualTo(Currency.USD);

        executeInTransaction(() -> {
            authService.updateCurrency(user, Currency.EUR);
        });

        User updated = authService.authenticate("testuser", "pass");
        assertThat(updated.getCurrency()).isEqualTo(Currency.EUR);
    }
}