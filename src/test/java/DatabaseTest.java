import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class DatabaseTest {
    protected EntityManagerFactory emf; // Прибрали static
    protected EntityManager em;

    @BeforeEach
    void initDb() {
        // Тепер фабрика (і чиста база) створюється заново для кожного тесту
        emf = Persistence.createEntityManagerFactory("balarama-test");
        em = emf.createEntityManager();
    }

    @AfterEach
    void closeDb() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close(); // Закриваємо фабрику, щоб H2 очистив пам'ять
        }
    }

    protected void executeInTransaction(Runnable action) {
        boolean wasActive = em.getTransaction().isActive();
        if (!wasActive) {
            em.getTransaction().begin();
        }
        try {
            action.run();
            if (!wasActive) {
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            if (!wasActive && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }
}