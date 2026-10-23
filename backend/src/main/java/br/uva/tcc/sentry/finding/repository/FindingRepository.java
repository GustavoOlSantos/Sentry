package br.uva.tcc.sentry.finding.repository;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.shared.domain.Severity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, UUID> {

    List<Finding> findByScanId(UUID scanId);

    List<Finding> findByAssetId(UUID assetId);

    List<Finding> findByServiceId(UUID serviceId);

    List<Finding> findByStatus(FindingStatus status);

    List<Finding> findBySeverity(Severity severity);

    List<Finding> findByType(FindingType type);
}
