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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        classes = {
                AchievementServiceApp.class
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
public class BaseContextTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
    
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18-alpine");

    static Network testNetwork = Network.newNetwork();

    @Container
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer(POSTGRES_IMAGE)
                .withNetwork(testNetwork)
                .withNetworkAliases("test-postgres")		        
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);

    static {
        POSTGRESQL_CONTAINER.start();
    }            

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
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
        if (testNetwork != null) {
            testNetwork.close();
        }
    }
}
