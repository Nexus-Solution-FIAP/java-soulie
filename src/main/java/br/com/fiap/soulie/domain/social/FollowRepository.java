package br.com.fiap.soulie.domain.social;

import br.com.fiap.soulie.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);

    Optional<Follow> findByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);

    long countByFollowee_Id(Long followeeId);

    long countByFollower_Id(Long followerId);

    /**
     * Quem me segue. Só contas ativas.
     *
     * <p>O {@code @EntityGraph} não é otimização: estes métodos são lidos fora
     * de transação, e sem ele o {@code follower} viria como proxy destacado e o
     * primeiro {@code getName()} estouraria {@code LazyInitializationException}.
     */
    @EntityGraph(attributePaths = "follower")
    Page<Follow> findByFollowee_IdAndFollower_ActiveTrue(Long followeeId, Pageable pageable);

    /** Quem eu sigo. Só contas ativas. */
    @EntityGraph(attributePaths = "followee")
    Page<Follow> findByFollower_IdAndFollowee_ActiveTrue(Long followerId, Pageable pageable);

    /**
     * Os dois se seguem?
     *
     * <p>É a consulta quente do grafo — ranking, feed e cutucada passam por ela
     * — e é o que define "amizade" neste projeto, já que amizade não tem tabela.
     */
    @Query("""
            select count(f) = 2 from Follow f
             where (f.follower.id = :umId and f.followee.id = :outroId)
                or (f.follower.id = :outroId and f.followee.id = :umId)
            """)
    boolean saoAmigos(@Param("umId") Long umId, @Param("outroId") Long outroId);

    /**
     * Usuários ativos que seguem de volta quem os segue.
     *
     * <p>Devolve o {@code User} direto, não o {@code Follow}: quem chama quer a
     * pessoa, e trazer a entidade já resolvida evita o acesso lazy fora de
     * transação.
     */
    @Query("""
            select f.followee from Follow f
             where f.follower.id = :userId
               and f.followee.active = true
               and exists (select 1 from Follow volta
                            where volta.follower.id = f.followee.id
                              and volta.followee.id = :userId)
             order by f.followee.name asc
            """)
    List<User> amigosDe(@Param("userId") Long userId);

    /** Só os ids, para quando a consulta seguinte filtra por eles. */
    @Query("""
            select f.followee.id from Follow f
             where f.follower.id = :userId
               and f.followee.active = true
               and exists (select 1 from Follow volta
                            where volta.follower.id = f.followee.id
                              and volta.followee.id = :userId)
            """)
    List<Long> idsDosAmigos(@Param("userId") Long userId);
}
