package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ValidationRepository extends JpaRepository<Validation, Long> {

    boolean existsByEvidenceMissionIdAndResultValidation(Long missionId, ResultValidation resultValidation);

    Optional<Validation> findByEvidenceId(Long evidenceId);

    /**
     * Carrega a validação já com evidência, dono e missão do app.
     *
     * <p>{@code MissionApiService.validate} não é {@code @Transactional} (o
     * upload e a espera da IA não podem reter conexão do pool) e chama
     * {@code statusDaValidacao} no mesmo bean — a anotação transacional dali
     * não vale, porque é auto-invocação. Sem o {@code join fetch}, a evidência
     * volta como proxy {@code LAZY} e o acesso a {@code user} estoura
     * {@code LazyInitializationException}: 500 em todo envio de foto.
     */
    @Query("""
            select v from Validation v
              join fetch v.evidence e
              join fetch e.user
              left join fetch e.dailyMission
             where v.id = :id
            """)
    Optional<Validation> findDetalheById(@Param("id") Long id);

    List<Validation> findAllByResultValidation(ResultValidation resultValidation);

    /**
     * Transiciona a validação de PENDING para o resultado final, **uma vez só**.
     *
     * <p>Esta é a trava de idempotência do pipeline inteiro. Reentrega de
     * mensagem é comportamento normal do RabbitMQ — acontece em reconexão, em
     * ack perdido, em redeploy no meio do processamento. Se o worker apenas
     * lesse a validação, checasse o estado em Java e gravasse, duas entregas
     * simultâneas passariam as duas pela checagem e **creditariam o XP duas
     * vezes**. É o mesmo problema do duplo gasto no resgate de recompensa, e a
     * solução é a mesma: deixar o banco decidir, numa instrução só.
     *
     * <p>Quem chama <b>tem</b> de conferir o retorno: {@code 0} significa que
     * outra entrega já resolveu esta validação, e nada mais deve ser feito —
     * nem XP, nem push.
     *
     * @return 1 se esta chamada fez a transição, 0 se já estava resolvida
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Validation v
               set v.resultValidation = :resultado,
                   v.resolvedAt = CURRENT_TIMESTAMP
             where v.id = :validationId
               and v.resultValidation = br.com.fiap.soulie.domain.validation.enums.ResultValidation.PENDING
            """)
    int resolveIfPending(@Param("validationId") Long validationId,
                         @Param("resultado") ResultValidation resultado);
}
