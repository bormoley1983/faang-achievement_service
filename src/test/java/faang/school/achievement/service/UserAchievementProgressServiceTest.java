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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    // ------------------------------------------------------------------ create

    @Test
    void create_whenUserAndAchievementExist_persistsAndReturnsProgress() {
        // Arrange (Set): fixed collaborators and a freshly created progress row
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 0);
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.of(achievement));
        when(progressRepository.findByUserIdAndAchievementId(USER_ID, ACHIEVEMENT_ID))
                .thenReturn(Optional.of(progress));

        // Act (Execute)
        AchievementProgress result = service.create(USER_ID, ACHIEVEMENT_ID);

        // Assert: returned row is the one read back; validation and idempotent insert happened
        assertThat(result).isSameAs(progress);
        verify(userValidator).validateUserExists(USER_ID);
        verify(progressRepository).createProgressIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void create_whenUserMissing_propagatesValidationFailureWithoutWriting() {
        // Arrange (Set): validator rejects the user before any repository call
        doThrow(new IllegalArgumentException("User not found: " + USER_ID))
                .when(userValidator).validateUserExists(USER_ID);

        // Act (Execute) / Assert: typed failure, no achievement lookup or write
        assertThatThrownBy(() -> service.create(USER_ID, ACHIEVEMENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found: " + USER_ID);

        verify(achievementRepository, never()).findById(anyLong());
        verify(progressRepository, never()).createProgressIfNecessary(anyLong(), anyLong());
    }

    @Test
    void create_whenAchievementMissing_throwsNotFoundWithoutWriting() {
        // Arrange (Set): user is valid, achievement lookup misses
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.empty());

        // Act (Execute) / Assert: typed failure with the achievement id in the message
        assertThatThrownBy(() -> service.create(USER_ID, ACHIEVEMENT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Achievement not found: " + ACHIEVEMENT_ID);

        verify(progressRepository, never()).createProgressIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void create_whenInsertDidNotMaterializeRow_throwsIllegalState() {
        // Arrange (Set): idempotent insert ran but the read-back finds nothing
        Achievement achievement = achievement(10);
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.of(achievement));
        when(progressRepository.findByUserIdAndAchievementId(USER_ID, ACHIEVEMENT_ID))
                .thenReturn(Optional.empty());

        // Act (Execute) / Assert: deterministic failure naming both ids
        assertThatThrownBy(() -> service.create(USER_ID, ACHIEVEMENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Progress was not created for user " + USER_ID + " and achievement " + ACHIEVEMENT_ID);

        verify(progressRepository).createProgressIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    // ------------------------------------------------------------- addProgress

    @Test
    void addProgress_whenPointsZero_rejectsBeforeAnyRemoteOrDatabaseCall() {
        // Act (Execute) / Assert: boundary value rejected with the exact contract message
        assertThatThrownBy(() -> service.addProgress(USER_ID, ACHIEVEMENT_ID, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Progress points must be positive");

        verifyNoInteractions(userValidator, achievementRepository, progressRepository, userAchievementRepository);
    }

    @Test
    void addProgress_whenPointsNegative_rejectsBeforeAnyRemoteOrDatabaseCall() {
        // Act (Execute) / Assert: negative boundary rejected identically to zero
        assertThatThrownBy(() -> service.addProgress(USER_ID, ACHIEVEMENT_ID, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Progress points must be positive");

        verifyNoInteractions(userValidator, achievementRepository, progressRepository, userAchievementRepository);
    }

    @Test
    void addProgress_whenUserMissing_propagatesValidationFailureWithoutWriting() {
        // Arrange (Set): validator rejects the user after the points check passes
        doThrow(new IllegalArgumentException("User not found: " + USER_ID))
                .when(userValidator).validateUserExists(USER_ID);

        // Act (Execute) / Assert: typed failure, no achievement lookup or write
        assertThatThrownBy(() -> service.addProgress(USER_ID, ACHIEVEMENT_ID, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found: " + USER_ID);

        verify(achievementRepository, never()).findById(anyLong());
        verify(progressRepository, never()).createProgressIfNecessary(anyLong(), anyLong());
    }

    @Test
    void addProgress_whenAchievementMissing_throwsNotFoundWithoutWriting() {
        // Arrange (Set): user valid, achievement lookup misses
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.empty());

        // Act (Execute) / Assert: typed failure before any progress write
        assertThatThrownBy(() -> service.addProgress(USER_ID, ACHIEVEMENT_ID, 3))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Achievement not found: " + ACHIEVEMENT_ID);

        verify(progressRepository, never()).createProgressIfNecessary(anyLong(), anyLong());
    }

    @Test
    void addProgress_whenAchievementPointsNonPositive_throwsIllegalStateWithoutWriting() {
        // Arrange (Set): corrupted achievement row with zero required points
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.of(achievement(0)));

        // Act (Execute) / Assert: typed failure naming the achievement id, no write
        assertThatThrownBy(() -> service.addProgress(USER_ID, ACHIEVEMENT_ID, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Achievement points must be positive: " + ACHIEVEMENT_ID);

        verify(progressRepository, never()).createProgressIfNecessary(anyLong(), anyLong());
    }

    @Test
    void addProgress_whenLockedRowMissing_throwsIllegalState() {
        // Arrange (Set): idempotent insert ran but the pessimistic read-back finds nothing
        when(achievementRepository.findById(ACHIEVEMENT_ID)).thenReturn(Optional.of(achievement(10)));
        when(progressRepository.findByUserIdAndAchievementIdForUpdate(USER_ID, ACHIEVEMENT_ID))
                .thenReturn(Optional.empty());

        // Act (Execute) / Assert: deterministic failure naming both ids
        assertThatThrownBy(() -> service.addProgress(USER_ID, ACHIEVEMENT_ID, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Progress was not created for user " + USER_ID + " and achievement " + ACHIEVEMENT_ID);

        verify(progressRepository).createProgressIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgress_whenBelowThreshold_persistsPartialProgressWithoutAwarding() {
        // Arrange (Set): 3 of 10 points already earned, +4 stays below threshold
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 3);
        stubLockedProgress(achievement, progress);

        // Act (Execute)
        service.addProgress(USER_ID, ACHIEVEMENT_ID, 4);

        // Assert: points advanced exactly by the increment; no award emitted
        assertThat(progress.getCurrentPoints()).isEqualTo(7);
        verify(progressRepository).save(progress);
        verify(userAchievementRepository, never()).createAwardIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgress_whenExactlyReachingThreshold_capsAndAwards() {
        // Arrange (Set): 8 of 10 points already earned, +2 lands exactly on threshold
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 8);
        stubLockedProgress(achievement, progress);

        // Act (Execute)
        service.addProgress(USER_ID, ACHIEVEMENT_ID, 2);

        // Assert: exact-threshold boundary awards and persists the capped value
        assertThat(progress.getCurrentPoints()).isEqualTo(10);
        verify(progressRepository).save(progress);
        verify(userAchievementRepository).createAwardIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgress_whenOverThreshold_capsAtRequiredPointsAndAwards() {
        // Arrange (Set): 8 of 10 points already earned, +5 overshoots the threshold
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 8);
        stubLockedProgress(achievement, progress);

        // Act (Execute)
        service.addProgress(USER_ID, ACHIEVEMENT_ID, 5);

        // Assert: increment is capped at the remaining points; award emitted once
        assertThat(progress.getCurrentPoints()).isEqualTo(10);
        verify(progressRepository).save(progress);
        verify(userAchievementRepository).createAwardIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgress_whenAlreadyComplete_repairsMissingAwardWithoutChangingProgress() {
        // Arrange (Set): progress already at threshold, award row missing
        Achievement achievement = achievement(10);
        AchievementProgress progress = progress(achievement, 10);
        stubLockedProgress(achievement, progress);

        // Act (Execute)
        service.addProgress(USER_ID, ACHIEVEMENT_ID, 1);

        // Assert: idempotent repair — no save of unchanged points, award insert attempted
        assertThat(progress.getCurrentPoints()).isEqualTo(10);
        verify(progressRepository, never()).save(progress);
        verify(userAchievementRepository).createAwardIfNecessary(USER_ID, ACHIEVEMENT_ID);
    }

    @Test
    void addProgress_whenNewUserReachesThreshold_savesBeforeAwarding() {
        // Arrange (Set): fresh progress row at 0, single increment covers the whole threshold
        Achievement achievement = achievement(3);
        AchievementProgress progress = progress(achievement, 0);
        stubLockedProgress(achievement, progress);

        // Act (Execute)
        service.addProgress(USER_ID, ACHIEVEMENT_ID, 3);

        // Assert: save happens before the award insert (ordering contract)
        assertThat(progress.getCurrentPoints()).isEqualTo(3);
        var order = inOrder(progressRepository, userAchievementRepository);
        order.verify(progressRepository).save(progress);
        order.verify(userAchievementRepository).createAwardIfNecessary(USER_ID, ACHIEVEMENT_ID);
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
