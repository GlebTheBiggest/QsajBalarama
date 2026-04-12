import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.CategoryGroupRepository;
import com.qsaj.balarama.v1.repository.CategoryItemRepository;
import com.qsaj.balarama.v1.repository.impl.CategoryGroupRepositoryImpl;
import com.qsaj.balarama.v1.repository.impl.CategoryItemRepositoryImpl;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryServiceImplTest extends DatabaseTest {

    private CategoryServiceImpl categoryService;
    private User testUser;

    @Mock
    private EventService eventService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CategoryGroupRepository groupRepo = new CategoryGroupRepositoryImpl(() -> em);
        CategoryItemRepository itemRepo = new CategoryItemRepositoryImpl(() -> em);
        categoryService = new CategoryServiceImpl(groupRepo, itemRepo, () -> em, eventService);

        executeInTransaction(() -> {
            testUser = User.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password("hash")
                    .build();
            em.persist(testUser);
        });
    }

    @Test
    void testCreateAndRetrieveGroup() {
        CategoryGroup group = CategoryGroup.builder()
                .name("Food")
                .type(TransactionType.EXPENSE)
                .color("#FF5733")
                .user(testUser)
                .build();

        categoryService.saveGroup(group);

        var groups = categoryService.getGroupsForUser(testUser);
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getName()).isEqualTo("Food");
    }

    @Test
    void testCreateAndRetrieveItem() {
        CategoryGroup group = CategoryGroup.builder()
                .name("Food")
                .type(TransactionType.EXPENSE)
                .color("#FF5733")
                .user(testUser)
                .build();
        categoryService.saveGroup(group);

        CategoryItem item = CategoryItem.builder()
                .name("Groceries")
                .group(group)
                .build();
        categoryService.saveItem(item);

        var items = categoryService.getItemsForGroup(group.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getName()).isEqualTo("Groceries");
    }

    @Test
    void testDeleteGroupWithCascade() {
        CategoryGroup group = CategoryGroup.builder()
                .name("Food")
                .type(TransactionType.EXPENSE)
                .color("#FF5733")
                .user(testUser)
                .build();
        categoryService.saveGroup(group);

        CategoryItem item = CategoryItem.builder()
                .name("Groceries")
                .group(group)
                .build();
        categoryService.saveItem(item);

        categoryService.deleteGroup(group.getId());

        var groups = categoryService.getGroupsForUser(testUser);
        assertThat(groups).isEmpty();
    }
}