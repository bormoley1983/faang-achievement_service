package faang.school.achievement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementServiceAppTests {

    @Test
    void userServiceDefaultPortMatchesUserServiceRuntimePort() throws IOException {
        var properties = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml"))
                .getFirst();

        assertThat(properties.getProperty("user-service.port"))
                .isEqualTo("${USER_SERVICE_PORT:8080}");
    }
}
