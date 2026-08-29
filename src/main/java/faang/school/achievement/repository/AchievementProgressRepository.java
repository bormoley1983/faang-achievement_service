package faang.school.achievement.repository;

import faang.school.achievement.model.AchievementProgress;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementProgressRepository extends CrudRepository<AchievementProgress, Long> {

    @Query(value = """
            SELECT ap
            FROM AchievementProgress ap
            WHERE ap.userId = :userId AND ap.achievement.id = :achievementId
    """)
    Optional<AchievementProgress> findByUserIdAndAchievementId(long userId, long achievementId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ap
            FROM AchievementProgress ap
            JOIN FETCH ap.achievement
            WHERE ap.userId = :userId AND ap.achievement.id = :achievementId
            """)
    Optional<AchievementProgress> findByUserIdAndAchievementIdForUpdate(long userId, long achievementId);

    @Query(nativeQuery = true, value = """
            INSERT INTO user_achievement_progress (user_id, achievement_id, current_points)
            VALUES (:userId, :achievementId, 0)
            ON CONFLICT DO NOTHING
    """)
    @Modifying
    void createProgressIfNecessary(long userId, long achievementId);

    List<AchievementProgress> findByUserId(long userId);
}
