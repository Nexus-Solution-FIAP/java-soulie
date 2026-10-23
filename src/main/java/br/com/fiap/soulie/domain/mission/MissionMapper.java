package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.domain.mission.dto.MissionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MissionMapper {

    @Mapping(target = "actionId", source = "action.id")
    @Mapping(target = "actionName", source = "action.name")
    @Mapping(target = "points", source = "action.points")
    MissionResponse toMissionResponse(Mission mission);
}
