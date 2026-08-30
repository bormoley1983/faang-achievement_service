package faang.school.achievement.client;

import faang.school.achievement.config.context.UserContext;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class FeignUserInterceptorTest {

    private static final long USER_ID = 7L;

    private UserContext userContext;
    private FeignUserInterceptor interceptor;

    @BeforeEach
    void setUp() {
        userContext = new UserContext();
        interceptor = new FeignUserInterceptor(userContext);
    }

    @AfterEach
    void tearDown() {
        userContext.clear();
    }

    @Test
    void apply_whenUserContextSet_forwardsUserIdAsHeader() {
        // Arrange (Set): context holder carries the caller identity
        userContext.setUserId(USER_ID);
        RequestTemplate template = new RequestTemplate();

        // Act (Execute)
        interceptor.apply(template);

        // Assert: outbound request carries the x-user-id header with the exact value
        Collection<String> values = template.headers().get("x-user-id");
        assertThat(values).containsExactly(String.valueOf(USER_ID));
    }
}
