package br.com.fiap.soulie.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findAllByUser_IdAndReadAtIsNullOrderByCreatedAtAsc(Long userId);

    List<UserNotification> findAllByUser_IdAndIdIn(Long userId, Collection<Long> ids);
}
