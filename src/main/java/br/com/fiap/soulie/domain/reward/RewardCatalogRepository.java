package br.com.fiap.soulie.domain.reward;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardCatalogRepository extends JpaRepository<RewardCatalog, Long> {

    List<RewardCatalog> findAllByActiveTrueOrderByCostAsc();
}
