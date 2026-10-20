package br.uva.tcc.sentry.scan.repository;

import br.uva.tcc.sentry.scan.domain.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ScanRepository extends JpaRepository<Scan, UUID> {
    // Custom query methods can be defined here
}