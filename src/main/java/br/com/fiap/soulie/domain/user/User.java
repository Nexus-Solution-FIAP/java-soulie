package br.com.fiap.soulie.domain.user;

import br.com.fiap.soulie.domain.user.record.UpdateUserData;
import br.com.fiap.soulie.domain.user.record.UserData;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "User")
@Table(name = "users")
public class User implements UserDetails {

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword(){
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void encryptPassword(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }
}
