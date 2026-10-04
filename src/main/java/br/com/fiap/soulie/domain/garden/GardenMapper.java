package br.com.fiap.soulie.domain.garden;

import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.garden.dto.GardenRequest;
import br.com.fiap.soulie.domain.garden.dto.GardenResponse;
import br.com.fiap.soulie.domain.user.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface GardenMapper {

    GardenResponse toGardenResponse(Garden garden);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "users", ignore = true)
    Garden toGarden(GardenRequest request);

    GardenResponse toGardenAvatarResponse(Avatar avatar);
}
