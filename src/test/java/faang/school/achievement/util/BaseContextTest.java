package faang.school.achievement.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.achievement.AchievementServiceApp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        classes = {
                AchievementServiceApp.class
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class BaseContextTest {
    private static final boolean CI_INTEGRATION =
            Boolean.parseBoolean(System.getenv("FAANG_CI_INTEGRATION"));
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
    
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18-alpine");

    private static final Network TEST_NETWORK;

    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer POSTGRESQL_CONTAINER;

    static {
        if (CI_INTEGRATION) {
            TEST_NETWORK = null;
            POSTGRESQL_CONTAINER = null;
        } else {
            TEST_NETWORK = Network.newNetwork();
            POSTGRESQL_CONTAINER = new PostgreSQLContainer(POSTGRES_IMAGE)
                    .withNetwork(TEST_NETWORK)
                    .withNetworkAliases("test-postgres")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");
            POSTGRESQL_CONTAINER.start();
        }
    }

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {

        if (CI_INTEGRATION) {
            registry.add("spring.datasource.url", () -> requiredEnvironment("FAANG_TEST_POSTGRES_URL"));
            registry.add("spring.datasource.username", () -> requiredEnvironment("FAANG_TEST_POSTGRES_USER"));
            registry.add("spring.datasource.password", () -> environment("FAANG_TEST_POSTGRES_PASSWORD", ""));
        } else {
            registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        }
        registry.add("spring.datasource.hikari.schema", () -> "public");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
        registry.add("spring.liquibase.default-schema", () -> "public");
        registry.add("spring.liquibase.liquibase-schema", () -> "public");
        
    }

    @AfterAll
    static void cleanup() {
        if (POSTGRESQL_CONTAINER != null && POSTGRESQL_CONTAINER.isRunning()) {
            POSTGRESQL_CONTAINER.stop();
        }
        if (TEST_NETWORK != null) {
            TEST_NETWORK.close();
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required CI integration setting: " + name);
        }
        return value;
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
