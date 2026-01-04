package com.pm.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.orderservice.model.DeadLetterEvent;
import com.pm.orderservice.model.EventType;
import com.pm.orderservice.model.OutboxEvent;
import com.pm.orderservice.repository.DeadLetterEventRepository;
import com.pm.orderservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutBoxProcessor Unit Tests")
class OutBoxProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private DeadLetterEventRepository deadLetterEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutBoxProcessor outBoxProcessor;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    @Captor
    private ArgumentCaptor<DeadLetterEvent> deadLetterEventCaptor;

    private OutboxEvent createTestEvent() {
        return OutboxEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .aggregateType("ORDER")
                .eventType(EventType.ORDER_CREATED)
                .payload("{\"orderId\":\"test-order-id\"}")
                .published(false)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private OutboxEvent createTestEventWithRetries(int retryCount) {
        OutboxEvent event = createTestEvent();
        event.setRetryCount(retryCount);
        return event;
    }

    // ========================================
    // PROCESS METHOD TESTS
    // ========================================

    @Nested
    @DisplayName("process() Tests")
    class ProcessTests {

        @Test
        @DisplayName("Should do nothing when no unpublished events exist")
        void process_WhenNoEvents_ShouldLogAndReturn() {
            // Arrange
            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(Collections.emptyList());

            // Act
            outBoxProcessor.process();

            // Assert
            verify(outboxEventRepository, times(1)).findTop100ByPublishedFalseOrderByCreatedAt();
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
            verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should successfully publish event to Kafka")
        void process_WhenEventExists_ShouldPublishToKafka() {
            // Arrange
            OutboxEvent event = createTestEvent();
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // Act
            outBoxProcessor.process();

            // Assert
            verify(kafkaTemplate, times(1)).send(
                    eq("order-events"),
                    eq(event.getAggregateId().toString()),
                    eq(event.getPayload())
            );
            verify(outboxEventRepository, times(1)).save(outboxEventCaptor.capture());

            OutboxEvent savedEvent = outboxEventCaptor.getValue();
            assertTrue(savedEvent.isPublished());
            assertNotNull(savedEvent.getPublishedAt());
        }

        @Test
        @DisplayName("Should process multiple events successfully")
        void process_WhenMultipleEvents_ShouldPublishAll() {
            // Arrange
            OutboxEvent event1 = createTestEvent();
            OutboxEvent event2 = createTestEvent();
            CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event1, event2));
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

            // Act
            outBoxProcessor.process();

            // Assert
            verify(kafkaTemplate, times(2)).send(anyString(), anyString(), anyString());
            verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should increment retry count on Kafka failure")
        void process_WhenKafkaFails_ShouldIncrementRetryCount() {
            // Arrange
            OutboxEvent event = createTestEvent();
            event.setRetryCount(0);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Kafka connection failed"));

            // Act
            outBoxProcessor.process();

            // Assert - Only 1 save happens when retrying (not exceeding max retries)
            verify(outboxEventRepository, times(1)).save(outboxEventCaptor.capture());

            OutboxEvent savedEvent = outboxEventCaptor.getValue();
            assertEquals(1, savedEvent.getRetryCount());
            assertFalse(savedEvent.isPublished());
        }

        @Test
        @DisplayName("Should move event to DLQ after exceeding max retries")
        void process_WhenMaxRetriesExceeded_ShouldMoveToDeadLetterQueue() {
            // Arrange
            OutboxEvent event = createTestEventWithRetries(3); // Already at max
            CompletableFuture<SendResult<String, String>> dlqFuture = CompletableFuture.completedFuture(null);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(eq("order-events"), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Kafka connection failed"));
            when(kafkaTemplate.send(eq("order-events-dlq"), anyString(), anyString()))
                    .thenReturn(dlqFuture);

            // Act
            outBoxProcessor.process();

            // Assert
            // Verify event was sent to DLQ topic
            verify(kafkaTemplate, times(1)).send(
                    eq("order-events-dlq"),
                    eq(event.getAggregateId().toString()),
                    eq(event.getPayload())
            );

            // Verify DeadLetterEvent was saved
            verify(deadLetterEventRepository, times(1)).save(deadLetterEventCaptor.capture());

            DeadLetterEvent dlqEvent = deadLetterEventCaptor.getValue();
            assertEquals(event.getEventId(), dlqEvent.getOriginalEventId());
            assertEquals(event.getAggregateId(), dlqEvent.getAggregateId());
            assertEquals(event.getPayload(), dlqEvent.getPayload());
            assertFalse(dlqEvent.isResolved());
            assertNotNull(dlqEvent.getFailureReason());
        }

        @Test
        @DisplayName("Should mark original event as published after moving to DLQ")
        void process_WhenMovedToDLQ_ShouldMarkOriginalAsPublished() {
            // Arrange
            OutboxEvent event = createTestEventWithRetries(3);
            CompletableFuture<SendResult<String, String>> dlqFuture = CompletableFuture.completedFuture(null);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(eq("order-events"), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Kafka connection failed"));
            when(kafkaTemplate.send(eq("order-events-dlq"), anyString(), anyString()))
                    .thenReturn(dlqFuture);

            // Act
            outBoxProcessor.process();

            // Assert
            // The last save should mark it as published
            verify(outboxEventRepository, atLeast(1)).save(outboxEventCaptor.capture());

            List<OutboxEvent> savedEvents = outboxEventCaptor.getAllValues();
            // Find the final state of the event
            OutboxEvent finalState = savedEvents.get(savedEvents.size() - 1);
            assertTrue(finalState.isPublished());
        }

        @Test
        @DisplayName("Should continue processing other events when one fails")
        void process_WhenOneEventFails_ShouldContinueWithOthers() {
            // Arrange
            OutboxEvent failingEvent = createTestEvent();
            OutboxEvent successEvent = createTestEvent();
            CompletableFuture<SendResult<String, String>> successFuture = CompletableFuture.completedFuture(null);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(failingEvent, successEvent));

            // First call fails, second succeeds
            when(kafkaTemplate.send(eq("order-events"), eq(failingEvent.getAggregateId().toString()), anyString()))
                    .thenThrow(new RuntimeException("Kafka failed"));
            when(kafkaTemplate.send(eq("order-events"), eq(successEvent.getAggregateId().toString()), anyString()))
                    .thenReturn(successFuture);

            // Act
            outBoxProcessor.process();

            // Assert
            verify(kafkaTemplate, times(2)).send(eq("order-events"), anyString(), anyString());
            verify(outboxEventRepository, atLeast(2)).save(any(OutboxEvent.class));
        }
    }

    // ========================================
    // DLQ FAILURE HANDLING TESTS
    // ========================================

    @Nested
    @DisplayName("DLQ Failure Handling Tests")
    class DlqFailureTests {

        @Test
        @DisplayName("Should handle DLQ publish failure gracefully")
        void process_WhenDLQPublishFails_ShouldLogError() {
            // Arrange
            OutboxEvent event = createTestEventWithRetries(3);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(eq("order-events"), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Main topic failed"));
            when(kafkaTemplate.send(eq("order-events-dlq"), anyString(), anyString()))
                    .thenThrow(new RuntimeException("DLQ also failed"));

            // Act - Should not throw
            assertDoesNotThrow(() -> outBoxProcessor.process());

            // Assert
            verify(kafkaTemplate, times(1)).send(eq("order-events-dlq"), anyString(), anyString());
            // DLQ event save should not be called if DLQ publish fails
        }
    }

    // ========================================
    // RETRY COUNT PROGRESSION TESTS
    // ========================================

    @Nested
    @DisplayName("Retry Count Progression Tests")
    class RetryCountTests {

        @Test
        @DisplayName("Should increment retry count from 0 to 1")
        void process_FirstFailure_ShouldSetRetryCountTo1() {
            // Arrange
            OutboxEvent event = createTestEventWithRetries(0);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Failed"));

            // Act
            outBoxProcessor.process();

            // Assert
            verify(outboxEventRepository, times(1)).save(outboxEventCaptor.capture());
            assertEquals(1, outboxEventCaptor.getValue().getRetryCount());
        }

        @Test
        @DisplayName("Should increment retry count from 2 to 3")
        void process_ThirdFailure_ShouldSetRetryCountTo3() {
            // Arrange
            OutboxEvent event = createTestEventWithRetries(2);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Failed"));

            // Act
            outBoxProcessor.process();

            // Assert
            verify(outboxEventRepository, times(1)).save(outboxEventCaptor.capture());
            assertEquals(3, outboxEventCaptor.getValue().getRetryCount());
        }

        @Test
        @DisplayName("Should not retry after max retries exceeded - move to DLQ instead")
        void process_FourthFailure_ShouldMoveToDLQ() {
            // Arrange
            OutboxEvent event = createTestEventWithRetries(3);
            CompletableFuture<SendResult<String, String>> dlqFuture = CompletableFuture.completedFuture(null);

            when(outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt())
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(eq("order-events"), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Failed"));
            when(kafkaTemplate.send(eq("order-events-dlq"), anyString(), anyString()))
                    .thenReturn(dlqFuture);

            // Act
            outBoxProcessor.process();

            // Assert
            verify(deadLetterEventRepository, times(1)).save(any(DeadLetterEvent.class));
        }
    }
}
