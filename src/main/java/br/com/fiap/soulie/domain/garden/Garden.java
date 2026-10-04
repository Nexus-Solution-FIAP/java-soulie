package br.com.fiap.soulie.domain.garden;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Garden")
@Table(name = "gardens")
public class Garden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id")
//    private User user;

    @ManyToMany
    @JoinTable(
            name = "gardens_users",
            joinColumns = @JoinColumn(name = "garden_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> users = new HashSet<>();

    public void addUserToGarden(User user) {
        this.users.add(user);
    }


}
