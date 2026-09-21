package br.com.fiap.soulie.avatar;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;


@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Avatar")
@Table(name = "TB_AVATAR")
public class Avatar {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idAvatar;

    private long idUser;
    private String name;
    private int level;

    @Enumerated(EnumType.STRING)
    private Humor humor;

    public Avatar(AvatarData avatarData) {
        this.name = avatarData.name();
        this.level = avatarData.level();
    }
}
