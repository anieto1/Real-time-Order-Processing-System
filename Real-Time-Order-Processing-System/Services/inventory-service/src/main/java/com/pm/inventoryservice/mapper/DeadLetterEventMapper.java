package com.pm.inventoryservice.mapper;

import com.pm.inventoryservice.dto.eventDTO.DeadLetterEventDTO;
import com.pm.inventoryservice.model.DeadLetterEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DeadLetterEventMapper {

    DeadLetterEvent toEntity(DeadLetterEventDTO dto);
    DeadLetterEventDTO toDTO(DeadLetterEvent entity);
    List<DeadLetterEventDTO> toDTOList(List<DeadLetterEvent> entities);
}
