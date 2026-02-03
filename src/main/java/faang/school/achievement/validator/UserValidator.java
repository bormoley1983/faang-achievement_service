package faang.school.achievement.validator;

import faang.school.achievement.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserServiceClient userServiceClient;

    public void validateUserExists(long userId) {
        if (!userServiceClient.userExists(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
    }
}