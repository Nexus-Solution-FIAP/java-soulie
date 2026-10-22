package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Validation")
@Table(name = "validations")
public class Validation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_id")
    private Evidence evidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_validation", nullable = false, length = 20)
    private ResultValidation resultValidation;

    public Validation(Evidence evidence) {
        this.evidence = evidence;
        this.resultValidation = ResultValidation.PENDING;
    }
}
