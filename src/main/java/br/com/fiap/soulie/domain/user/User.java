package br.com.fiap.soulie.domain.user;

import br.com.fiap.soulie.domain.user.record.UpdateUserData;
import br.com.fiap.soulie.domain.user.record.UserData;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "User")
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;
    private int points;
    private Boolean active;

    public User(UserData userData) {
        this.name = userData.name();
        this.email = userData.email();
        this.password = userData.password();
        this.points = 0;
        this.active = true;
    }

    public void updateData(@Valid UpdateUserData updateUserData) {
        if(updateUserData.name() != null ){
            this.name = updateUserData.name();
        }

        if(updateUserData.email() != null){
            this.email = updateUserData.email();
        }

        // Aqui precisa passar por uma validação,
        // o usuário não pode simplesmente mudar a senha quando ele quiser.
        if(updateUserData.password() != null){
            this.password = updateUserData.password();
        }
    }

    public void deleteUser() {
        this.active = false;
    }

    public void activateUser() {
        this.active = true;
    }

}
