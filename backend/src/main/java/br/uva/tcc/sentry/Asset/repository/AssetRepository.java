package br.uva.tcc.sentry.Asset.repository;

import br.uva.tcc.sentry.Asset.DTO.AssetResponse;
import br.uva.tcc.sentry.Asset.domain.Asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;



public interface AssetRepository extends JpaRepository<Asset, UUID> {
    @EntityGraph(attributePaths = "services")
    @Query("SELECT a FROM Asset a")
    List<Asset> findAllWithServices();

    @EntityGraph(attributePaths = "services")
    @Query("SELECT a FROM Asset a WHERE a.id = :id")
    Optional<AssetResponse> findByIdWithServices(UUID id);
}
