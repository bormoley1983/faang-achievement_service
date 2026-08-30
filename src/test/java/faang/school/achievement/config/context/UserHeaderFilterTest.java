package faang.school.achievement.config.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class UserHeaderFilterTest {

    private static final long USER_ID = 7L;

    private UserContext userContext;
    private UserHeaderFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        userContext = spy(new UserContext());
        filter = new UserHeaderFilter(userContext);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        userContext.clear();
    }

    @Test
    void doFilter_whenHeaderPresent_setsUserContextAndClearsItAfterwards() throws ServletException, IOException {
        // Arrange (Set): request carries the caller identity header
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-user-id", String.valueOf(USER_ID));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act (Execute)
        filter.doFilter(request, response, chain);

        // Assert: downstream ran with the same pair; context set for the call and cleared in finally
        verify(chain).doFilter(request, response);
        verify(userContext).setUserId(USER_ID);
        verify(userContext).clear();
    }

    @Test
    void doFilter_whenDownstreamFails_stillClearsUserContext() throws ServletException, IOException {
        // Arrange (Set): downstream handler blows up mid-request
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-user-id", String.valueOf(USER_ID));
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new ServletException("downstream failure")).when(chain).doFilter(request, response);

        // Act (Execute) / Assert: failure propagates but the ThreadLocal is cleaned up in finally
        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class)
                .hasMessage("downstream failure");

        verify(userContext).clear();
    }

    @Test
    void doFilter_whenHeaderMissing_rejectsBeforeReachingDownstream() throws ServletException, IOException {
        // Arrange (Set): request without the required identity header
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act (Execute) / Assert: typed rejection, chain never invoked, context untouched
        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x-user-id");

        verify(chain, never()).doFilter(any(), any());
        verify(userContext, never()).setUserId(any(Long.class));
    }
}
