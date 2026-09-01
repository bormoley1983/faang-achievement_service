package faang.school.achievement.repository;

import faang.school.achievement.model.Achievement;
import org.springframework.data.repository.CrudRepository;

public interface AchievementRepository extends CrudRepository<Achievement, Long> {
}
