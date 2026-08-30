package faang.school.achievement.controller;

import faang.school.achievement.config.context.UserContext;
import faang.school.achievement.service.UserAchievementProgressService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AchievementProgressControllerTest {

    private static final long USER_ID = 7L;
    private static final long ACHIEVEMENT_ID = 11L;

    @Mock
    private UserAchievementProgressService progressService;
    @Mock
    private UserContext userContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AchievementProgressController(progressService, userContext))
                .build();
    }

    @Test
    void addProgress_whenValidRequest_forwardsUserContextAndReturnsNoContent() throws Exception {
        // Arrange (Set): user context resolves the caller identity
        Mockito.when(userContext.getUserId()).thenReturn(USER_ID);

        // Act (Execute) / Assert: 204 and the exact (user, achievement, points) forwarding contract
        mockMvc.perform(post("/api/v1/achievements/{achievementId}/progress", ACHIEVEMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":5}"))
                .andExpect(status().isNoContent());

        verify(progressService).addProgress(USER_ID, ACHIEVEMENT_ID, 5);
    }

    @Test
    void addProgress_whenPointsMissing_returnsBadRequestWithoutCallingService() throws Exception {
        // Act (Execute) / Assert: bean validation rejects the payload before the service seam
        mockMvc.perform(post("/api/v1/achievements/{achievementId}/progress", ACHIEVEMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(progressService, never()).addProgress(anyLong(), anyLong(), anyInt());
    }

    @Test
    void addProgress_whenPointsNonPositive_returnsBadRequestWithoutCallingService() throws Exception {
        // Act (Execute) / Assert: @Positive boundary rejected before the service seam
        mockMvc.perform(post("/api/v1/achievements/{achievementId}/progress", ACHIEVEMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":0}"))
                .andExpect(status().isBadRequest());

        verify(progressService, never()).addProgress(anyLong(), anyLong(), anyInt());
    }

    @Test
    void addProgress_whenMalformedJson_returnsBadRequestWithoutCallingService() throws Exception {
        // Act (Execute) / Assert: unparseable body rejected before the service seam
        mockMvc.perform(post("/api/v1/achievements/{achievementId}/progress", ACHIEVEMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest());

        verify(progressService, never()).addProgress(anyLong(), anyLong(), anyInt());
    }

    @Test
    void addProgress_whenUserContextMissing_propagatesAsInternalError() throws Exception {
        // Arrange (Set): no x-user-id header reached the context holder
        Mockito.when(userContext.getUserId()).thenThrow(new IllegalStateException("no user context"));

        // Act (Execute) / Assert: typed failure propagates out of the controller, service never invoked
        assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/achievements/{achievementId}/progress", ACHIEVEMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":5}")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .rootCause()
                .hasMessage("no user context");

        verify(progressService, never()).addProgress(anyLong(), anyLong(), anyInt());
    }

    @Test
    void addProgress_whenAchievementMissing_propagatesNotFoundAsInternalError() throws Exception {
        // Arrange (Set): service reports the achievement as unknown
        Mockito.when(userContext.getUserId()).thenReturn(USER_ID);
        doThrow(new NoSuchElementException("Achievement not found: " + ACHIEVEMENT_ID))
                .when(progressService).addProgress(USER_ID, ACHIEVEMENT_ID, 5);

        // Act (Execute) / Assert: without a global exception handler the typed failure propagates as-is
        assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/achievements/{achievementId}/progress", ACHIEVEMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":5}")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(NoSuchElementException.class)
                .rootCause()
                .hasMessage("Achievement not found: " + ACHIEVEMENT_ID);

        verify(progressService).addProgress(USER_ID, ACHIEVEMENT_ID, 5);
    }

}
