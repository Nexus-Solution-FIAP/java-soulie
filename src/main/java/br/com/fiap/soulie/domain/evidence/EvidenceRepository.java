package br.com.fiap.soulie.domain.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findAllByMissionId(Long missionId);

    /**
     * Já existe uma evidência deste usuário para esta missão do app aguardando a IA?
     *
     * <p>Sem esta checagem, o fluxo {@code /v1} não tinha teto nenhum: cada
     * chamada a {@code POST /v1/missions/{id}/validate} gravava um objeto de até
     * 10MB no bucket, uma {@code Evidence}, uma {@code Validation} e uma
     * mensagem na fila — ou seja, N uploads e <b>N chamadas pagas à IA</b> por
     * um único usuário em laço. O {@code alreadyDoneInPeriod} não barrava, porque
     * ele só é verdadeiro depois de uma aprovação.
     *
     * <p>O fluxo legado já tinha o equivalente, no
     * {@code NoPendingEvidenceValidator} — mas aquela cadeia tipa {@code Mission}
     * e por isso não alcança o fluxo novo.
     *
     * <p><b>O corte por {@code pendenteDesde} e o que impede o teto de virar
     * cadeado.</b> Uma pendencia que nunca se resolve — publicacao perdida entre
     * o commit e a fila, processo morto no meio — bloquearia a missao para
     * sempre. Passado o corte, a pendencia deixa de contar e o usuario pode
     * enviar de novo. O caminho normal de falha e resolvido como {@code FAILED}
     * pelo consumidor da dead-letter queue; isto e a rede embaixo dele.
     */
    @Query("""
            select count(e) > 0 from Evidence e
              join Validation v on v.evidence = e
             where e.user.id = :userId
               and e.dailyMission.id = :dailyMissionId
               and v.resultValidation = br.com.fiap.soulie.domain.validation.enums.ResultValidation.PENDING
               and v.requestedAt >= :pendenteDesde
            """)
    boolean existsPendingForDailyMission(@Param("userId") Long userId,
                                         @Param("dailyMissionId") Long dailyMissionId,
                                         @Param("pendenteDesde") LocalDateTime pendenteDesde);

    /**
     * Quantas fotos este usuario ja mandou desta missao na janela.
     *
     * <p>O teto de "uma pendente por vez" nao limitava o total: {@code REJECTED}
     * libera reenvio imediato, e nao havia contador. Dava para reenviar a mesma
     * foto em laco esperando um falso positivo da IA — e cada ciclo e um objeto
     * de ate 10MB no bucket mais uma <b>chamada paga</b> a API do parceiro, na
     * conta do projeto. Com milhares de ciclos por hora e varias missoes em
     * paralelo, a aprovacao por acaso e questao de tempo.
     */
    @Query("""
            select count(e) from Evidence e
             where e.user.id = :userId
               and e.dailyMission.id = :dailyMissionId
               and e.submittedAt >= :desde
            """)
    long contarEnviosNaJanela(@Param("userId") Long userId,
                              @Param("dailyMissionId") Long dailyMissionId,
                              @Param("desde") LocalDateTime desde);
}
