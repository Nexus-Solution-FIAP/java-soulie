package br.com.fiap.soulie.domain.avatar;


import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.avatar.dto.AvatarRequest;
import br.com.fiap.soulie.domain.avatar.dto.UpdateAvatar;
import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Avatar")
@Table(name = "avatars")
public class Avatar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int level;

    @Enumerated(EnumType.STRING)
    private Humor humor;

    @Enumerated(EnumType.STRING)
    private PlantType plantType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Avatar(AvatarRequest avatarRequest, User user) {
        this.name = avatarRequest.name();
        this.user = user;
        this.level = 0;
        this.humor = Humor.NEUTRAL;
        this.plantType = PlantType.IPE_AMARELO;
    }

    public void updateAvatar(@Valid UpdateAvatar updateAvatarData){
        if (updateAvatarData.name() != null){
            this.name = updateAvatarData.name();
        }

        if (updateAvatarData.plantType() != null) {
            this.plantType = updateAvatarData.plantType();
        }
    }

}
