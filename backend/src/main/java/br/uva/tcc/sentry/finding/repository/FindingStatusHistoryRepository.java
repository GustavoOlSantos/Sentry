package br.uva.tcc.sentry.finding.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.uva.tcc.sentry.finding.domain.FindingStatusHistory;

public interface FindingStatusHistoryRepository extends JpaRepository<FindingStatusHistory, Long> {
}
