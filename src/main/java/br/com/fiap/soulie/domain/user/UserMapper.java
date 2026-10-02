package br.com.fiap.soulie.domain.user;

import br.com.fiap.soulie.domain.user.dto.UserRequest;
import br.com.fiap.soulie.domain.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toDetail(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "points", constant = "0")
    @Mapping(target = "active", constant = "true")
    User toEntity(UserRequest userRequest);
}
