package faang.school.achievement.validator;

import faang.school.achievement.client.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    private static final long USER_ID = 42L;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private UserValidator userValidator;

    @Test
    void validateUserExists_whenUserExists_passesWithoutError() {
        // Arrange (Set): remote client confirms the user exists
        when(userServiceClient.userExists(USER_ID)).thenReturn(true);

        // Act (Execute) / Assert: no exception, exactly one lookup
        assertThatCode(() -> userValidator.validateUserExists(USER_ID))
                .doesNotThrowAnyException();

        verify(userServiceClient).userExists(USER_ID);
    }

    @Test
    void validateUserExists_whenUserMissing_throwsTypedFailure() {
        // Arrange (Set): remote client reports the user as absent
        when(userServiceClient.userExists(USER_ID)).thenReturn(false);

        // Act (Execute) / Assert: rejection carries the user id in the message
        assertThatThrownBy(() -> userValidator.validateUserExists(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found: " + USER_ID);

        verify(userServiceClient).userExists(USER_ID);
    }

    @Test
    void validateUserExists_whenClientFails_propagatesFailure() {
        // Arrange (Set): remote client call blows up (timeout, 5xx, etc.)
        when(userServiceClient.userExists(USER_ID))
                .thenThrow(new IllegalStateException("user-service unavailable"));

        // Act (Execute) / Assert: failure propagates unchanged — no translation, no retry here
        assertThatThrownBy(() -> userValidator.validateUserExists(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("user-service unavailable");

        verify(userServiceClient).userExists(USER_ID);
    }
}
