package faang.school.achievement.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementEntityMethodsTest {

    @Test
    void bidirectionalGraphDoesNotParticipateInGeneratedEntityMethods() {
        Achievement achievement = Achievement.builder().id(1L).build();
        AchievementProgress progress = AchievementProgress.builder()
                .id(2L)
                .achievement(achievement)
                .build();
        UserAchievement award = UserAchievement.builder()
                .id(3L)
                .achievement(achievement)
                .build();
        achievement.setProgresses(List.of(progress));
        achievement.setUserAchievements(List.of(award));

        assertThat(achievement.toString()).startsWith(Achievement.class.getName() + "@");
        assertThat(progress.toString()).startsWith(AchievementProgress.class.getName() + "@");
        assertThat(award.toString()).startsWith(UserAchievement.class.getName() + "@");
        assertThat(achievement).isNotEqualTo(Achievement.builder().id(1L).build());
    }
}
