package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_mission_completions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "mission_id", "completed_on"})
)
public class UserMissionCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id")
    private DailyMission mission;

    @Column(name = "completed_on", nullable = false)
    private LocalDate completedOn;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "image_name")
    private String imageName;

    public UserMissionCompletion() {
    }

    public static UserMissionCompletion of(User user, DailyMission mission, LocalDateTime when, String imageName) {
        var completion = new UserMissionCompletion();
        completion.user = user;
        completion.mission = mission;
        completion.completedOn = when.toLocalDate();
        completion.completedAt = when;
        completion.imageName = imageName;
        return completion;
    }
}
