package com.pm.orderservice.kafka.consumers;


import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeadLetterQueueConsumer {
    @KafkaListener(
            topics = "order-events-dlq",
            groupId = "order-service-dlq-monitor"
    )
    public void handleDeadLetterEvent(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String aggregateId,
            Acknowledgment acknowledgment) {
        log.error("DLQ EVENT: aggregateID" + "={} payload={}", aggregateId, payload);
        acknowledgment.acknowledge();
    }


}
