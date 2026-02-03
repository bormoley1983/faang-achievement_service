package faang.school.achievement.service;

import faang.school.achievement.model.AchievementProgress;
import faang.school.achievement.repository.AchievementProgressRepository;
import faang.school.achievement.repository.AchievementRepository;
import faang.school.achievement.validator.UserValidator;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

//TODO
@Service
@RequiredArgsConstructor
public class UserAchievementProgressService {
    private final AchievementProgressRepository achievementProgressRepository;
    private final UserValidator userValidator; 
    private final AchievementRepository achievementRepository;

    public AchievementProgress create(long userId, long achievementId) {
        userValidator.validateUserExists(userId);

        return new AchievementProgress();
    }

    public void addProgress(long userId, long achievementId, int points) {
        userValidator.validateUserExists(userId);  
    }
}
