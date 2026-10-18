package br.com.fiap.soulie.domain.avatar;

import br.com.fiap.soulie.domain.avatar.dto.AvatarRequest;
import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AvatarMapper {

    @Mapping(target = "idSoulie", source = "id")
    @Mapping(target = "nameSoulie", source = "name")
    AvatarResponse toDetail(Avatar avatar);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "avatarRequest.name")
    @Mapping(target = "humor", constant = "NEUTRAL")
    @Mapping(target = "level", constant = "0")
    @Mapping(target = "xp", constant = "0")
    @Mapping(target = "flowerType", constant = "GIRASSOL")
    @Mapping(target = "lastWateredAt", ignore = true)
    Avatar toEntity(AvatarRequest avatarRequest, User user);

}
