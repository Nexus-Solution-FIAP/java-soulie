package br.com.fiap.soulie.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findAllByActiveTrue(Pageable pageable);

    List<User> findAllByActiveTrueOrderByPointsDescIdAsc();

    long countByActiveTrueAndPointsGreaterThan(int points);

    boolean existsByHandle(String handle);

    boolean existsByEmail(String email);

    User findByEmail(String email);

    List<User> findAllByExpoPushToken(String expoPushToken);

    /**
     * Debita pontos de forma atômica, no banco.
     *
     * <p>Existe porque ler o saldo, validar e gravar em passos separados é uma
     * corrida: duas requisições simultâneas de resgate leem {@code points = 100},
     * as duas passam pelo {@code SufficientPointsValidator}, as duas calculam
     * {@code 100 - 100} em memória e as duas gravam {@code 0} — gastando o mesmo
     * saldo duas vezes e gerando dois resgates. A guarda dentro de
     * {@link User#spendPoints} não resolve isso: ela só impede saldo negativo
     * dentro de **uma** transação.
     *
     * <p>Aqui a leitura e a escrita acontecem numa só instrução, e o
     * {@code AND u.points >= :amount} faz o próprio banco recusar o segundo
     * débito. Quem chama **tem** de conferir o retorno: {@code 0} significa que
     * o saldo já não era suficiente e a operação precisa ser abortada.
     *
     * @return quantas linhas foram atualizadas — 1 em caso de sucesso, 0 se o
     *         saldo era insuficiente no momento do débito
     */
    @Modifying
    @Query("update User u set u.points = u.points - :amount where u.id = :userId and u.points >= :amount")
    int debitPoints(@Param("userId") Long userId, @Param("amount") int amount);
}
