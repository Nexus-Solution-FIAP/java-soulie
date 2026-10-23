package br.com.fiap.soulie.domain.user;

import br.com.fiap.soulie.domain.user.dto.UserRequest;
import br.com.fiap.soulie.domain.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // O nome vem de publicName(), que respeita o showFullName do usuário.
    @Mapping(target = "name", expression = "java(user.publicName())")
    UserResponse toDetail(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "points", constant = "0")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "handle", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "streakDays", constant = "0")
    @Mapping(target = "lastActivityDate", ignore = true)
    @Mapping(target = "co2SavedKg", constant = "0")
    @Mapping(target = "waterSavedLiters", constant = "0")
    @Mapping(target = "notificationsEnabled", constant = "true")
    @Mapping(target = "emailWeeklyDigest", constant = "false")
    @Mapping(target = "expoPushToken", ignore = true)
    @Mapping(target = "allowNudges", constant = "true")
    @Mapping(target = "showFullName", constant = "true")
    @Mapping(target = "gardens", ignore = true)
    User toEntity(UserRequest userRequest);
}
