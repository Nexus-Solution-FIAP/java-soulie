package br.com.fiap.soulie.user;

import br.com.fiap.soulie.adress.Address;
import jakarta.persistence.*;
import lombok.*;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "User")
@Table(name = "TB_USUARIOS")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idUser;

    private String name;
    private String email;
    private String password;
    private int points;

    @Embedded
    private Address address;

    public User(UserData userData) {
        this.name = userData.name();
        this.email = userData.email();
        this.password = userData.password();
        this.points = userData.points();
    }
}
