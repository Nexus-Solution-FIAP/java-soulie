package br.com.fiap.soulie.domain.avatar.record;

import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.user.record.UserDataDetail;

public record AvatarDataDetail(

        Long idSoulie,

        String nameSoulie,

        int level,

        Humor humor,

        PlantType plantType,

        UserDataDetail user

) {
    public AvatarDataDetail(Avatar avatar) {
        this(
                avatar.getId(),
                avatar.getName(),
                avatar.getLevel(),
                avatar.getHumor(),
                avatar.getPlantType(),
                new UserDataDetail(avatar.getUser())
        );
    }
}
