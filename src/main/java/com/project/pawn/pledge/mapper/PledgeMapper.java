package com.project.pawn.pledge.mapper;

import com.project.pawn.pledge.dto.ItemDto;
import com.project.pawn.pledge.dto.OrnamentDto;
import com.project.pawn.pledge.model.Item;
import com.project.pawn.pledge.model.Ornament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PledgeMapper {

    OrnamentDto toOrnamentDto(Ornament entity);

    List<OrnamentDto> toOrnamentDtoList(List<Ornament> entities);

    @Mapping(target = "createdAt", ignore = true)
    Ornament toOrnamentEntity(OrnamentDto dto);

    @Mapping(target = "status",
            expression = "java(entity.getStatus() != null ? com.project.pawn.pledge.enums.ItemStatus.valueOf(entity.getStatus()) : null)")
    @Mapping(target = "outstandingBalance", expression = "java(entity.getOutstandingBalance())")
    @Mapping(target = "ornamentType", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    ItemDto toItemDto(Item entity);

    List<ItemDto> toItemDtoList(List<Item> entities);
}
