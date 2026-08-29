package faang.school.achievement.service;

import faang.school.achievement.client.UserServiceClient;
import faang.school.achievement.model.Achievement;
import faang.school.achievement.model.AchievementProgress;
import faang.school.achievement.model.Rarity;
import faang.school.achievement.repository.AchievementProgressRepository;
import faang.school.achievement.repository.AchievementRepository;
import faang.school.achievement.repository.UserAchievementRepository;
import faang.school.achievement.util.BaseContextTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAchievementProgressServiceIT extends BaseContextTest {

    private static final long USER_ID = 9_000_001L;

    @Autowired
    private UserAchievementProgressService service;
    @Autowired
    private AchievementRepository achievementRepository;
    @Autowired
    private AchievementProgressRepository progressRepository;
    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @Test
    void ordinalRaritySeedDataIsMigratedToStableEnumNames() {
        assertThat(StreamSupport.stream(achievementRepository.findAll().spliterator(), false))
                .filteredOn(achievement -> achievement.getTitle().equals("HANDSOME"))
                .singleElement()
                .extracting(Achievement::getRarity)
                .isEqualTo(Rarity.COMMON);
        assertThat(StreamSupport.stream(achievementRepository.findAll().spliterator(), false))
                .filteredOn(achievement -> achievement.getTitle().equals("MR PRODUCTIVITY"))
                .singleElement()
                .extracting(Achievement::getRarity)
                .isEqualTo(Rarity.LEGENDARY);
    }

    @Test
    void concurrentUpdatesDoNotLoseProgressAndCreateOneAward() throws Exception {
        when(userServiceClient.userExists(USER_ID)).thenReturn(true);
        Achievement achievement = achievementRepository.findAll().iterator().next();
        long achievementId = achievement.getId();
        long requiredPoints = achievement.getPoints();
        int firstIncrement = Math.toIntExact(Math.max(1, requiredPoints / 2));
        int secondIncrement = Math.toIntExact(requiredPoints - firstIncrement);

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(() -> addAfterSignal(start, achievementId, firstIncrement));
            Future<?> second = executor.submit(() -> addAfterSignal(start, achievementId, secondIncrement));

            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }

        AchievementProgress progress = progressRepository
                .findByUserIdAndAchievementId(USER_ID, achievementId)
                .orElseThrow();
        assertThat(progress.getCurrentPoints()).isEqualTo(requiredPoints);
        assertThat(userAchievementRepository.findByUserId(USER_ID))
                .singleElement()
                .extracting(userAchievement -> userAchievement.getAchievement().getId())
                .isEqualTo(achievementId);
    }

    @Test
    void inboundHttpContractPersistsProgressBeforeReportingSuccess() throws Exception {
        long httpUserId = USER_ID + 1;
        when(userServiceClient.userExists(httpUserId)).thenReturn(true);
        Achievement achievement = achievementRepository.findAll().iterator().next();

        mockMvc.perform(post("/api/v1/achievements/{achievementId}/progress", achievement.getId())
                        .header("x-user-id", httpUserId)
                        .contentType("application/json")
                        .content("{\"points\":1}"))
                .andExpect(status().isNoContent());

        assertThat(progressRepository.findByUserIdAndAchievementId(httpUserId, achievement.getId()))
                .get()
                .extracting(AchievementProgress::getCurrentPoints)
                .isEqualTo(1L);
    }

    private void addAfterSignal(CountDownLatch start, long achievementId, int points) {
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to start concurrent update");
            }
            service.addProgress(USER_ID, achievementId, points);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent update was interrupted", exception);
        }
    }
}
