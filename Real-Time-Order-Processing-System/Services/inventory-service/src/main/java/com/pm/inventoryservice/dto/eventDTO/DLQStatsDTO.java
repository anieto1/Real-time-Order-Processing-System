package com.pm.inventoryservice.dto.eventDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DLQStatsDTO {

    private Long totalUnresolved;
    private Long totalResolved;
    private Duration oldestUnresolvedAge;
    private Map<String, Long> failureReasonBreakdown;
}
