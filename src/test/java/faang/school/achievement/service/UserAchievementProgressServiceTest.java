package faang.school.achievement.service;

import faang.school.achievement.model.Achievement;
import faang.school.achievement.model.AchievementProgress;
import faang.school.achievement.repository.AchievementProgressRepository;
import faang.school.achievement.repository.AchievementRepository;
import faang.school.achievement.repository.UserAchievementRepository;
import faang.school.achievement.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAchievementProgressServiceTest {

    private static final long USER_ID = 7L;
    private static final long ACHIEVEMENT_ID = 11L;

    @Mock
    private AchievementProgressRepository progressRepository;
    @Mock
    private UserAchievementRepository userAchievementRepository;
    @Mock
    private UserValidator userValidator;
    @Mock
    private AchievementRepository achievementRepository;

    private UserAchievementProgressService service;

    @BeforeEach
    void setUp() {
        service = new UserAchievementProgressService(
                progressRepository,
                userAchievementRepository,
                userValidator,
                achievementRepository);
    }

    @Test
    void createPersistsAndReturnsProgress() {
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 0);
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.of(achievement));
        when(progressRepository.findByUserIdAndAchievementId(USER_ID, ACHIEVEMENT_ID))
                .thenReturn(Optional.of(progress));

        AchievementProgress result = service.create(USER_ID, ACHIEVEMENT_ID);

        assertThat(result).isSameAs(progress);
        verify(userValidator).validateUserExists(USER_ID);
        verify(progressRepository).createProgressIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void createRejectsUnknownAchievementWithoutWriting() {
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, ACHIEVEMENT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Achievement not found: " + ACHIEVEMENT_ID);

        verify(progressRepository, never()).createProgressIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgressPersistsPartialProgressWithoutAwarding() {
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 3);
        stubLockedProgress(achievement, progress);

        service.addProgress(USER_ID, ACHIEVEMENT_ID, 4);

        assertThat(progress.getCurrentPoints()).isEqualTo(7);
        verify(progressRepository).save(progress);
        verify(userAchievementRepository, never()).createAwardIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgressCapsAtThresholdAndAwards() {
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 8);
        stubLockedProgress(achievement, progress);

        service.addProgress(USER_ID, ACHIEVEMENT_ID, 5);

        assertThat(progress.getCurrentPoints()).isEqualTo(10);
        verify(progressRepository).save(progress);
        verify(userAchievementRepository).createAwardIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgressRepairsMissingAwardAtCompletedProgressWithoutChangingProgress() {
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 10);
        stubLockedProgress(achievement, progress);

        service.addProgress(USER_ID, ACHIEVEMENT_ID, 1);

        verify(progressRepository, never()).save(progress);
        verify(userAchievementRepository).createAwardIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgressRejectsNonPositivePointsBeforeAnyRemoteOrDatabaseCall() {
        assertThatThrownBy(() -> service.addProgress(USER_ID, ACHIEVEMENT_ID, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Progress points must be positive");

        verify(userValidator, never()).validateUserExists(USER_ID);
        verify(achievementRepository, never()).findById(ACHIEVEMENT_ID);
    }

    private void stubLockedProgress(Achievement achievement, AchievementProgress progress) {
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.of(achievement));
        when(progressRepository.findByUserIdAndAchievementIdForUpdate(USER_ID, ACHIEVEMENT_ID))
                .thenReturn(Optional.of(progress));
    }

    private Achievement achievement(long requiredPoints) {
        return Achievement.builder()
                .id(ACHIEVEMENT_ID)
                .points(requiredPoints)
                .build();
    }

    private AchievementProgress progress(Achievement achievement, long currentPoints) {
        return AchievementProgress.builder()
                .userId(USER_ID)
                .achievement(achievement)
                .currentPoints(currentPoints)
                .build();
    }
}
