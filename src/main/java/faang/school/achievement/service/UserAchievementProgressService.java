package faang.school.achievement.service;

import faang.school.achievement.model.Achievement;
import faang.school.achievement.model.AchievementProgress;
import faang.school.achievement.repository.AchievementProgressRepository;
import faang.school.achievement.repository.AchievementRepository;
import faang.school.achievement.repository.UserAchievementRepository;
import faang.school.achievement.validator.UserValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserAchievementProgressService {
    private final AchievementProgressRepository achievementProgressRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserValidator userValidator;
    private final AchievementRepository achievementRepository;

    @Transactional
    public AchievementProgress create(long userId, long achievementId) {
        userValidator.validateUserExists(userId);
        getAchievement(achievementId);

        achievementProgressRepository.createProgressIfNecessary(userId, achievementId);

        return achievementProgressRepository.findByUserIdAndAchievementId(userId, achievementId)
                .orElseThrow(() -> new IllegalStateException(
                        "Progress was not created for user %d and achievement %d"
                                .formatted(userId, achievementId)));
    }

    @Transactional
    public void addProgress(long userId, long achievementId, int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Progress points must be positive");
        }

        userValidator.validateUserExists(userId);
        var achievement = getAchievement(achievementId);
        if (achievement.getPoints() <= 0) {
            throw new IllegalStateException("Achievement points must be positive: " + achievementId);
        }

        achievementProgressRepository.createProgressIfNecessary(userId, achievementId);
        var progress = achievementProgressRepository
                .findByUserIdAndAchievementIdForUpdate(userId, achievementId)
                .orElseThrow(() -> new IllegalStateException(
                        "Progress was not created for user %d and achievement %d"
                                .formatted(userId, achievementId)));

        long requiredPoints = achievement.getPoints();
        long currentPoints = progress.getCurrentPoints();
        if (currentPoints < requiredPoints) {
            long increment = Math.min((long) points, requiredPoints - currentPoints);
            progress.setCurrentPoints(currentPoints + increment);
            achievementProgressRepository.save(progress);
        }

        if (progress.getCurrentPoints() >= requiredPoints) {
            userAchievementRepository.createAwardIfNecessary(userId, achievementId);
        }
    }

    private Achievement getAchievement(long achievementId) {
        return achievementRepository.findById(achievementId)
                .orElseThrow(() -> new NoSuchElementException("Achievement not found: " + achievementId));
    }
}
