package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Veredito da IA sobre uma evidência.
 *
 * <p>{@code evidence_id} é UNIQUE desde a V7: uma evidência tem no máximo uma
 * validação. É essa restrição que torna o fluxo idempotente — a reentrega de uma
 * mensagem do RabbitMQ não cria uma segunda validação para a mesma evidência.
 */
@Getter
@Builder
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Validation")
@Table(name = "validations")
public class Validation {

    /** Separador das detecções na coluna de texto. */
    private static final String SEPARADOR = "\n";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_id")
    private Evidence evidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_validation", nullable = false, length = 20)
    private ResultValidation resultValidation;

    /**
     * O que a IA reconheceu na imagem, uma por linha.
     *
     * <p>VARCHAR com tamanho, não {@code columnDefinition = "TEXT"}: TEXT é
     * tipo do PostgreSQL e o H2 do perfil de teste não o conhece, então o
     * schema dos testes nem era criado. Entidade e migration precisam concordar
     * — é a mesma regra registrada no CLAUDE.md.
     */
    @Column(length = 2000)
    private String detections;

    @Column
    private Double confidence;

    @Column(length = 500)
    private String reason;

    /** Qual modelo decidiu. Útil quando houver mais de um provedor. */
    @Column(length = 100)
    private String provider;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Quantas vezes já se tentou analisar. Cresce a cada reentrega. */
    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    public Validation(Evidence evidence) {
        this.evidence = evidence;
        this.resultValidation = ResultValidation.PENDING;
        this.requestedAt = LocalDateTime.now();
        this.attempts = 0;
    }

    public boolean isPending() {
        return resultValidation == ResultValidation.PENDING;
    }

    public boolean isApproved() {
        return resultValidation == ResultValidation.APPROVED;
    }

    /** Lista as detecções; vazia quando a IA não devolveu nenhuma. */
    public List<String> detectionList() {
        if (detections == null || detections.isBlank()) {
            return List.of();
        }
        return List.of(detections.split(SEPARADOR));
    }

    /**
     * Grava o veredito da IA.
     *
     * <p>Quem garante que isso roda uma vez só é o {@code UPDATE} condicional em
     * {@code ValidationRepository.resolveIfPending} — este método é o que
     * preenche os campos depois que aquele garantiu a transição.
     */
    public void resolver(ResultValidation resultado,
                         List<String> detections,
                         Double confidence,
                         String reason,
                         String provider) {
        this.resultValidation = resultado;
        this.detections = detections == null || detections.isEmpty()
                ? null
                : String.join(SEPARADOR, detections);
        this.confidence = confidence;
        this.reason = reason;
        this.provider = provider;
        this.resolvedAt = LocalDateTime.now();
    }

    public void registrarTentativa() {
        this.attempts++;
    }
}
