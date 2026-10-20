package br.com.fiap.soulie.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findAllByActiveTrue(Pageable pageable);

    List<User> findAllByActiveTrueOrderByPointsDescIdAsc();

    long countByActiveTrueAndPointsGreaterThan(int points);

    boolean existsByHandle(String handle);

    User findByEmail(String email);

    List<User> findAllByExpoPushToken(String expoPushToken);
}
