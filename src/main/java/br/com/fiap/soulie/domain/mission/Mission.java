package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.domain.action.Action;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.mission.dto.MissionRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Mission")
@Table(name = "missions")
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id")
    private Action action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_id")
    private Avatar avatar;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    private MissionStatus status;

    public Mission(MissionRequest missionRequest, Action action, Avatar avatar) {
        this.action = action;
        this.avatar = avatar;
        this.description = missionRequest.description();
        this.status = MissionStatus.ACTIVE;
    }

    // Evidência enviada: aguardando a IA validar
    public void startValidation() {
        this.status = MissionStatus.IN_PROGRESS;
    }

    // As regras de "pode concluir?" ficam em validations/mission
    public void complete() {
        this.status = MissionStatus.COMPLETED;
        this.avatar.addXp(this.action.getPoints());
    }
}
