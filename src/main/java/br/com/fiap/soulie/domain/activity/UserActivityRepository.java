package br.com.fiap.soulie.domain.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    List<UserActivity> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * Quanto XP de atividade livre este usuário já recebeu na janela.
     *
     * <p>Existe porque o teto por requisição não era teto nenhum. O
     * {@code @Max(50)} do DTO limita UMA chamada, e o número de chamadas não
     * tinha limite: 100 requisições davam 5000 XP, subiam nível, marcavam
     * streak e destravavam conquista — sem foto, sem evidência e sem passar
     * pela IA. O teto só vale se for agregado.
     */
    @Query("""
            select coalesce(sum(a.xp), 0) from UserActivity a
             where a.user.id = :userId
               and a.createdAt >= :inicio
               and a.createdAt < :fim
            """)
    int somaXpNaJanela(@Param("userId") Long userId,
                       @Param("inicio") LocalDateTime inicio,
                       @Param("fim") LocalDateTime fim);
}
