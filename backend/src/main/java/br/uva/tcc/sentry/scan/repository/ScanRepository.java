package br.uva.tcc.sentry.scan.repository;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ScanRepository extends JpaRepository<Scan, UUID> {
    @EntityGraph(attributePaths = {"execGroup", "warnings"})
    Optional<Scan> findWithExecGroupById(UUID id);

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM Scan s
        WHERE s.execGroup.id = :groupId
          AND s.status IN :statuses
        """)
        boolean existsScansByGroupIdAndStatusIn(@Param("groupId") UUID groupId, @Param("statuses") Collection<ScanStatus> statuses);
}