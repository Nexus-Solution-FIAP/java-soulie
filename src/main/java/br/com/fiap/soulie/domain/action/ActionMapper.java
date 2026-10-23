package br.com.fiap.soulie.domain.action;

import br.com.fiap.soulie.domain.action.dto.ActionRequest;
import br.com.fiap.soulie.domain.action.dto.ActionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActionMapper {

    ActionResponse toActionResponse(Action action);

    @Mapping(target = "id", ignore = true)
    Action toAction(ActionRequest request);
}
