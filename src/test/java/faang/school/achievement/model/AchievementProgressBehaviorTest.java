package faang.school.achievement.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementProgressBehaviorTest {

    @Test
    void increment_whenCalledOnce_raisesCurrentPointsByOne() {
        // Arrange (Set): progress row at a known point value
        AchievementProgress progress = AchievementProgress.builder()
                .userId(7L)
                .currentPoints(3)
                .build();

        // Act (Execute)
        progress.increment();

        // Assert: hand-written behavior advances exactly one point
        assertThat(progress.getCurrentPoints()).isEqualTo(4);
    }

    @Test
    void increment_whenCalledFromZero_startsAtOne() {
        // Arrange (Set): fresh progress row at the zero boundary
        AchievementProgress progress = AchievementProgress.builder()
                .userId(7L)
                .currentPoints(0)
                .build();

        // Act (Execute)
        progress.increment();

        // Assert: boundary value handled without underflow or skip
        assertThat(progress.getCurrentPoints()).isEqualTo(1);
    }
}
