package br.uva.tcc.sentry.scan.repository;

import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ScanExecutionGroupScanRepository extends JpaRepository<ScanExecutionGroup, UUID> {
    // Custom query methods can be defined here
}