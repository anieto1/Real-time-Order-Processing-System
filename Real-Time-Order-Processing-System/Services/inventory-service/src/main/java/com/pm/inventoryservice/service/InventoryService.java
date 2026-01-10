package com.pm.inventoryservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final InventoryMapper inventoryMapper;

}
