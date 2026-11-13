package br.uva.tcc.sentry.scan.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;

public interface ScanExecutionGroupRepository extends JpaRepository<ScanExecutionGroup, UUID> {

    /** Os 200 grupos mais recentes (a tela de varreduras não pagina). */
    List<ScanExecutionGroup> findTop200ByOrderByCreatedAtDesc();
}
