package br.uva.tcc.sentry.finding.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.uva.tcc.sentry.finding.domain.ScanFinding;

public interface ScanFindingRepository extends JpaRepository<ScanFinding, Long> {

    @Modifying
    @Query("DELETE FROM ScanFinding sf WHERE sf.scan.id = :scanId")
    int deleteByScanId(@Param("scanId") UUID scanId);
}
