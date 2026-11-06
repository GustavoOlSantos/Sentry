package br.uva.tcc.sentry.Asset.repository;

import br.uva.tcc.sentry.Asset.DTO.AssetSummaryResponse;
import br.uva.tcc.sentry.Asset.domain.Asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findByIpAddress(String ipAddress);

    @EntityGraph(attributePaths = "services")
    @Query("SELECT DISTINCT a FROM Asset a")
    List<Asset> findAllWithServices();

    @EntityGraph(attributePaths = "findings")
    @Query("SELECT DISTINCT a FROM Asset a")
    List<Asset> findAllWithFindings();

    @EntityGraph(attributePaths = "services")
    @Query("SELECT a FROM Asset a WHERE a.id = :id")
    Optional<Asset> findByIdWithServices(@Param("id") UUID id);

    @EntityGraph(attributePaths = "findings")
    @Query("SELECT a FROM Asset a WHERE a.id = :id")
    Optional<Asset> findByIdWithFindings(@Param("id") UUID id);

    @Query("""
        SELECT new br.uva.tcc.sentry.Asset.DTO.AssetSummaryResponse(
            a.id,
            a.ipAddress,
            a.hostname,
            a.macAddress,
            a.operatingSystem,
            a.status,
            a.firstSeenAt,
            a.lastSeenAt,
            (SELECT COUNT(s) FROM Service s WHERE s.asset = a AND s.state = 'open'),
            SIZE(a.findings)
        )
        FROM Asset a
    """)
    List<AssetSummaryResponse> findAllAssetSummary();
}
