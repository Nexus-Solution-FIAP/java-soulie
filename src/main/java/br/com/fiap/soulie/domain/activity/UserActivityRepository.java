package br.com.fiap.soulie.domain.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    List<UserActivity> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
