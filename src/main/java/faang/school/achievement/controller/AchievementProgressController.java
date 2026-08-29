package faang.school.achievement.controller;

import faang.school.achievement.config.context.UserContext;
import faang.school.achievement.dto.AddProgressRequest;
import faang.school.achievement.service.UserAchievementProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
public class AchievementProgressController {

    private final UserAchievementProgressService progressService;
    private final UserContext userContext;

    @PostMapping("/{achievementId}/progress")
    public ResponseEntity<Void> addProgress(
            @PathVariable long achievementId,
            @Valid @RequestBody AddProgressRequest request) {
        progressService.addProgress(userContext.getUserId(), achievementId, request.points());
        return ResponseEntity.noContent().build();
    }
}
