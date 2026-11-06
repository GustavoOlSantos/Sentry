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
