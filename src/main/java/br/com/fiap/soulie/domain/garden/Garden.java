package br.com.fiap.soulie.domain.garden;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Entity(name = "Garden")
@Table(name = "gardens")
public class Garden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 12)
    private String inviteCode;

    @ManyToMany
    @JoinTable(
            name = "gardens_users",
            joinColumns = @JoinColumn(name = "garden_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();

    public Garden() {
    }

    public void addUserToGarden(User user) {
        this.users.add(user);
    }


}
