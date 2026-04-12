import com.qsaj.balarama.v1.model.Event;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.EventType;
import com.qsaj.balarama.v1.repository.EventRepository;
import com.qsaj.balarama.v1.repository.impl.EventRepositoryImpl;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventServiceImplTest extends DatabaseTest {

    private EventService eventService;
    private User testUser;

    @BeforeEach
    void setUp() {
        EventRepository eventRepo = new EventRepositoryImpl(() -> em);
        eventService = new EventServiceImpl(eventRepo);

        executeInTransaction(() -> {
            testUser = User.builder()
                    .username("eventuser")
                    .email("event@example.com")
                    .password("hash")
                    .build();
            em.persist(testUser);
        });
    }

    @Test
    void testLogEvent() {
        eventService.logEvent(testUser, EventType.CATEGORY_CREATED, "Created category Food");

        List<Event> events = eventService.getRecentEvents(testUser, 10);
        assertThat(events).hasSize(1);
        Event event = events.get(0);
        assertThat(event.getMessage()).isEqualTo("Created category Food");
        assertThat(event.getEventType()).isEqualTo(EventType.CATEGORY_CREATED);
        assertThat(event.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testGetRecentEvents() {
        for (int i = 0; i < 15; i++) {
            eventService.logEvent(testUser, EventType.TRANSACTION_ADDED, "Transaction " + i);
        }

        List<Event> recent = eventService.getRecentEvents(testUser, 5);
        assertThat(recent).hasSize(5);
        // Очікуємо, що останні 5 (найновіші) – залежить від порядку сортування в репозиторії.
        // Перевіряємо, що повертаються останні 5.
        assertThat(recent.get(0).getMessage()).isEqualTo("Transaction 14");
    }
}