package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "interactions")
public class Interaction {

    public static final String NUDGE = "NUDGE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 20)
    private String kind;

    public Interaction() {
    }

    public static Interaction nudge(User sender, User receiver) {
        var interaction = new Interaction();
        interaction.sender = sender;
        interaction.receiver = receiver;
        interaction.kind = NUDGE;
        interaction.createdAt = LocalDateTime.now();
        return interaction;
    }
}
