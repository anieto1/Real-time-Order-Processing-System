package com.pm.inventoryservice.service;

import com.pm.inventoryservice.dto.eventDTO.DLQStatsDTO;
import com.pm.inventoryservice.dto.eventDTO.DeadLetterEventDTO;
import com.pm.inventoryservice.model.DeadLetterEvent;
import com.pm.inventoryservice.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final DeadLetterEventRepository deadLetterEventRepository;

    @Transactional(readOnly = true)
    public List<DeadLetterEventDTO> getUnresolvedEvents(){
        List<DeadLetterEvent> events = deadLetterEventRepository.findByResolvedFalseOrderByMovedToDlqAtDesc();

        
    }

    public DLQStatsDTO getStats(){}

    public boolean reprocessEvent(UUID dlqId){

    }

    void markAsResolved(UUID dlqId, String resolvedBy){}

}
