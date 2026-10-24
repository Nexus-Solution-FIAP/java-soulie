package br.com.fiap.soulie.domain.history_points;

import br.com.fiap.soulie.domain.action.Action;
import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "HistoryPoints")
@Table(name = "history_points")
public class HistoryPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id")
    private Action action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;
    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    public HistoryPoints(User user, Action action, int pointsEarned) {
        this.user = user;
        this.action = action;
        this.date = LocalDate.now();
        this.pointsEarned = pointsEarned;
    }

}
