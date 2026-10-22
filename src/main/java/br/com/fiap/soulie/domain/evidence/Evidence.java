package br.com.fiap.soulie.domain.evidence;

import br.com.fiap.soulie.domain.evidence.dto.EvidenceRequest;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Evidence")
@Table(name = "evidences")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @Column(nullable = false, length = 500)
    private String image;

    public Evidence(EvidenceRequest evidenceRequest, User user, Mission mission){
        this.user = user;
        this.mission = mission;
        this.image = evidenceRequest.image();
    }


}
