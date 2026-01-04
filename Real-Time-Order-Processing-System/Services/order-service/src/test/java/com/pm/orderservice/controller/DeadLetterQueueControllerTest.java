package com.pm.orderservice.controller;

import com.pm.orderservice.model.DeadLetterEvent;
import com.pm.orderservice.model.EventType;
import com.pm.orderservice.repository.DeadLetterEventRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeadLetterQueueController.class)
@DisplayName("DeadLetterQueueController Tests")
class DeadLetterQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeadLetterEventRepository deadLetterEventRepository;

    @MockBean
    private OutboxEventRepository outboxEventRepository;

    // ========================================
    // HELPER METHODS
    // ========================================

    private DeadLetterEvent createDLQEvent() {
        return DeadLetterEvent.builder()
                .dlqId(UUID.randomUUID())
                .originalEventId(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .eventType(EventType.ORDER_CREATED)
                .payload("{\"orderId\":\"test\"}")
                .retryCount(4)
                .failureReason("Kafka connection failed")
                .movedToDlqAt(LocalDateTime.now())
                .resolved(false)
                .build();
    }

    // ========================================
    // GET /api/admin/dlq/unresolved TESTS
    // ========================================

    @Nested
    @DisplayName("GET /api/admin/dlq/unresolved")
    class GetUnresolvedEventsTests {

        @Test
        @DisplayName("Should return list of unresolved DLQ events")
        void getUnresolvedEvents_WhenEventsExist_ShouldReturnList() throws Exception {
            // Arrange
            DeadLetterEvent event = createDLQEvent();

            when(deadLetterEventRepository.findByResolvedFalseOrderByMovedToDlqAtDesc())
                    .thenReturn(List.of(event));

            // Act & Assert
            mockMvc.perform(get("/api/admin/dlq/unresolved"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].dlqId").exists())
                    .andExpect(jsonPath("$[0].resolved").value(false))
                    .andExpect(jsonPath("$[0].failureReason").value("Kafka connection failed"));

            verify(deadLetterEventRepository, times(1))
                    .findByResolvedFalseOrderByMovedToDlqAtDesc();
        }

        @Test
        @DisplayName("Should return empty list when no unresolved events")
        void getUnresolvedEvents_WhenEmpty_ShouldReturnEmptyList() throws Exception {
            // Arrange
            when(deadLetterEventRepository.findByResolvedFalseOrderByMovedToDlqAtDesc())
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/admin/dlq/unresolved"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ========================================
    // GET /api/admin/dlq/stats TESTS
    // ========================================

    @Nested
    @DisplayName("GET /api/admin/dlq/stats")
    class GetStatsTests {

        @Test
        @DisplayName("Should return DLQ statistics")
        void getStats_ShouldReturnStats() throws Exception {
            // Arrange
            DeadLetterEvent unresolvedEvent = createDLQEvent();
            unresolvedEvent.setResolved(false);

            when(deadLetterEventRepository.count()).thenReturn(10L);
            when(deadLetterEventRepository.findByResolvedFalseOrderByMovedToDlqAtDesc())
                    .thenReturn(List.of(unresolvedEvent, unresolvedEvent, unresolvedEvent));

            // Act & Assert
            mockMvc.perform(get("/api/admin/dlq/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(10))
                    .andExpect(jsonPath("$.unresolvedCount").value(3))
                    .andExpect(jsonPath("$.resolvedCount").value(7));
        }

        @Test
        @DisplayName("Should return zero stats when no events")
        void getStats_WhenEmpty_ShouldReturnZeros() throws Exception {
            // Arrange
            when(deadLetterEventRepository.count()).thenReturn(0L);
            when(deadLetterEventRepository.findByResolvedFalseOrderByMovedToDlqAtDesc())
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/admin/dlq/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.unresolvedCount").value(0))
                    .andExpect(jsonPath("$.resolvedCount").value(0));
        }
    }

    // ========================================
    // GET /api/admin/dlq/{dlqId} TESTS
    // ========================================

    @Nested
    @DisplayName("GET /api/admin/dlq/{dlqId}")
    class GetDlqEventByIdTests {

        @Test
        @DisplayName("Should return DLQ event when found")
        void getDlqEvent_WhenExists_ShouldReturn200() throws Exception {
            // Arrange
            UUID dlqId = UUID.randomUUID();
            DeadLetterEvent event = createDLQEvent();
            event.setDlqId(dlqId);

            when(deadLetterEventRepository.findById(dlqId))
                    .thenReturn(Optional.of(event));

            // Act & Assert
            mockMvc.perform(get("/api/admin/dlq/{dlqId}", dlqId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dlqId").value(dlqId.toString()))
                    .andExpect(jsonPath("$.eventType").value("ORDER_CREATED"))
                    .andExpect(jsonPath("$.retryCount").value(4));

            verify(deadLetterEventRepository, times(1)).findById(dlqId);
        }

        @Test
        @DisplayName("Should return 404 when DLQ event not found")
        void getDlqEvent_WhenNotExists_ShouldReturn404() throws Exception {
            // Arrange
            UUID dlqId = UUID.randomUUID();

            when(deadLetterEventRepository.findById(dlqId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/admin/dlq/{dlqId}", dlqId))
                    .andExpect(status().isNotFound());

            verify(deadLetterEventRepository, times(1)).findById(dlqId);
        }

        @Test
        @DisplayName("Should return 500 when dlqId is invalid UUID (no specific handler)")
        void getDlqEvent_WithInvalidUUID_ShouldReturn500() throws Exception {
            // Act & Assert - Invalid UUID format falls through to generic exception handler
            mockMvc.perform(get("/api/admin/dlq/{dlqId}", "not-a-valid-uuid"))
                    .andExpect(status().isInternalServerError());

            verify(deadLetterEventRepository, never()).findById(any());
        }
    }
}
