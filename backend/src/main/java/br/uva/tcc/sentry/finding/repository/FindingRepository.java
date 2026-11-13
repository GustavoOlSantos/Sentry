package br.uva.tcc.sentry.finding.repository;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.shared.domain.Severity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FindingRepository extends JpaRepository<Finding, UUID> {

    List<Finding> findByScanId(UUID scanId);

    List<Finding> findByAssetId(UUID assetId);

    List<Finding> findByServiceId(UUID serviceId);

    List<Finding> findByStatus(FindingStatus status);

    List<Finding> findBySeverity(Severity severity);

    List<Finding> findByType(FindingType type);

    /** [groupId, findings] detectados pela primeira vez por scans de cada grupo. */
    @Query("""
        SELECT f.scan.execGroup.id, COUNT(f)
        FROM Finding f
        WHERE f.scan.execGroup.id IN :groupIds
        GROUP BY f.scan.execGroup.id
        """)
    List<Object[]> countByGroupIds(@Param("groupIds") Collection<UUID> groupIds);

    /** [scanId, findings] detectados pela primeira vez por cada scan. */
    @Query("""
        SELECT f.scan.id, COUNT(f)
        FROM Finding f
        WHERE f.scan.id IN :scanIds
        GROUP BY f.scan.id
        """)
    List<Object[]> countByScanIds(@Param("scanIds") Collection<UUID> scanIds);

    @Query("""
        SELECT f FROM Finding f
        JOIN FETCH f.asset
        LEFT JOIN FETCH f.service
        WHERE f.scan.id = :scanId
        """)
    List<Finding> findWithAssetByScanId(@Param("scanId") UUID scanId);

    /** Findings que já existem para os fingerprints observados num scan (em qualquer status). */
    List<Finding> findByFingerprintIn(Collection<String> fingerprints);

    /** Findings ainda "em jogo" nos assets varridos — candidatos a serem validados por ausência. */
    @Query("""
        SELECT f FROM Finding f
        LEFT JOIN FETCH f.service
        WHERE f.asset.id IN :assetIds
          AND f.status IN :statuses
        """)
    List<Finding> findTrackedByAssetIds(@Param("assetIds") Collection<UUID> assetIds,
                                        @Param("statuses") Collection<FindingStatus> statuses);
}
