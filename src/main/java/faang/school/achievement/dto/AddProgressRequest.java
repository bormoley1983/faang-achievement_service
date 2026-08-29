package faang.school.achievement.dto;

import jakarta.validation.constraints.Positive;

public record AddProgressRequest(
        @Positive(message = "points must be positive") int points) {
}
