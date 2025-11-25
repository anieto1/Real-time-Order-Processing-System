package com.pm.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.orderservice.model.OutboxEvent;
import com.pm.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutBoxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;


    @Transactional
    @Scheduled(fixedRate = 5000)
    public void processOutboxEvent(){
        List<OutboxEvent> outboxEvents = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAt();

        if(outboxEvents.isEmpty()) return;


        for(OutboxEvent outboxEvent : outboxEvents){
            try{
                kafkaTemplate.send("order-events", outboxEvent.getAggregateId().toString(), outboxEvent.getPayload());
                outboxEvent.setPublished(true);
                outboxEvent.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(outboxEvent);
            }catch (Exception e) {
                outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
                outboxEventRepository.save(outboxEvent);

                if (outboxEvent.getRetryCount() > 3) {

                }
                log.error("Error publishing event {} to kafka", outboxEvent.getEventId(), e);

            }


        }




    }

}
