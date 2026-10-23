package br.com.fiap.soulie.domain.evidence;

import br.com.fiap.soulie.domain.evidence.dto.EvidenceRequest;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.infra.storage.StoredImage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Foto enviada para comprovar uma missão.
 *
 * <p>Serve os **dois** modelos de missão que convivem no projeto: a
 * {@link Mission} legada (vinda do {@code soulie-soul-up}, com {@code Action} e
 * status) e a {@link DailyMission} do catálogo que o contrato {@code /v1}
 * consome. Exatamente um dos dois vínculos é preenchido, e o banco garante isso
 * com o {@code CHECK} da migration V23 — sem ele, "exatamente um" seria uma
 * convenção que alguém quebra sem perceber.
 *
 * <p><b>Ressalva que estava errada aqui:</b> o texto afirmava que aprovar a
 * evidência destrava o {@code PUT /mission/{id}/complete} legado. Não destrava,
 * porque a evidência legada guarda uma URL e não uma chave do bucket, e o
 * cliente da IA precisa da chave para gerar a URL assinada — então a validação
 * do fluxo legado nunca chega a {@code APPROVED} e o
 * {@code ApprovedEvidenceValidator} continua barrando. Levar a URL do cliente
 * para a IA seria SSRF com saída, então fazer o legado funcionar exige decidir
 * primeiro de onde vem aquela imagem. Está registrado como pendência.
 *
 * <p>O que o pipeline de fato alimenta hoje é o fluxo do app: aprovar credita a
 * recompensa do
 * {@code /v1}.
 */
@Getter
@Builder
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

    /** Preenchido no fluxo legado. Nulo no fluxo do app. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    /** Preenchido no fluxo do app. Nulo no fluxo legado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_mission_id")
    private DailyMission dailyMission;

    /**
     * Endereço de leitura da imagem.
     *
     * <p>No fluxo legado vem do cliente (validado como https). No fluxo do app
     * é montado pelo servidor a partir da chave, e fica nulo quando o bucket é
     * privado — aí a leitura é só por URL assinada.
     */
    @Column(nullable = false, length = 500)
    private String image;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Chave do objeto no bucket, gerada pelo servidor (UUID). */
    @Column(name = "image_key", length = 255)
    private String imageKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /** Fluxo legado: a imagem é uma URL que o cliente informou. */
    public Evidence(EvidenceRequest evidenceRequest, User user, Mission mission) {
        this.user = user;
        this.mission = mission;
        this.image = evidenceRequest.image();
        this.imageUrl = evidenceRequest.image();
        this.submittedAt = LocalDateTime.now();
    }

    /** Fluxo do app: a imagem foi enviada e guardada pelo servidor. */
    public static Evidence doAppParaMissaoDiaria(User user, DailyMission dailyMission, StoredImage imagem) {
        Evidence evidence = new Evidence();
        evidence.user = user;
        evidence.dailyMission = dailyMission;
        evidence.imageKey = imagem.key();
        evidence.imageUrl = imagem.url();
        evidence.contentType = imagem.contentType();
        evidence.sizeBytes = imagem.sizeBytes();
        // `image` é NOT NULL desde a V6 e é o campo que o fluxo legado usa. Com
        // bucket privado não há URL pública, então guardamos a chave — é o que
        // identifica o objeto de qualquer jeito.
        evidence.image = imagem.url() == null ? imagem.key() : imagem.url();
        evidence.submittedAt = LocalDateTime.now();
        return evidence;
    }

    public boolean isDoApp() {
        return dailyMission != null;
    }

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
