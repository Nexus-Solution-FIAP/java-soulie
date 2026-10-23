package br.com.fiap.soulie.domain.garden;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Builder
@AllArgsConstructor
@Entity(name = "Garden")
@Table(name = "gardens")
public class Garden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = 12)
    private String inviteCode;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "gardens_users",
            joinColumns = @JoinColumn(name = "garden_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();

    public Garden() {
        this.users = new HashSet<>();
    }

    public void addUserToGarden(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        boolean already = this.users.stream().anyMatch(member -> user.getId().equals(member.getId()));
        if (!already) {
            this.users.add(user);
        }
    }


}
