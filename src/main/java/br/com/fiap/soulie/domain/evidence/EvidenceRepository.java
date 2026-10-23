package br.com.fiap.soulie.domain.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     */
    @Query("""
            select count(e) > 0 from Evidence e
              join Validation v on v.evidence = e
             where e.user.id = :userId
               and e.dailyMission.id = :dailyMissionId
               and v.resultValidation = br.com.fiap.soulie.domain.validation.enums.ResultValidation.PENDING
            """)
    boolean existsPendingForDailyMission(@Param("userId") Long userId,
                                         @Param("dailyMissionId") Long dailyMissionId);
}
