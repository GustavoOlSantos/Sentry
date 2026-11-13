package br.uva.tcc.sentry.scan.repository;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScanRepository extends JpaRepository<Scan, UUID> {
    boolean existsByTargetAndStatus(String target, ScanStatus status);

    List<Scan> findTop200ByOrderByCreatedAtDesc();

    List<Scan> findByExecGroupIdOrderByCreatedAtAsc(UUID groupId);

    /** [groupId, status, quantidade] — quantos scans de cada status há em cada grupo. */
    @Query("""
        SELECT s.execGroup.id, s.status, COUNT(s)
        FROM Scan s
        WHERE s.execGroup.id IN :groupIds
        GROUP BY s.execGroup.id, s.status
        """)
    List<Object[]> countByGroupAndStatus(@Param("groupIds") Collection<UUID> groupIds);

    /** [groupId, target], na ordem de criação. */
    @Query("""
        SELECT s.execGroup.id, s.target
        FROM Scan s
        WHERE s.execGroup.id IN :groupIds
        ORDER BY s.createdAt ASC
        """)
    List<Object[]> findTargetsByGroupIds(@Param("groupIds") Collection<UUID> groupIds);

    /** [scanId, quantidade de avisos]. */
    @Query("""
        SELECT s.id, COUNT(w)
        FROM Scan s JOIN s.warnings w
        WHERE s.id IN :scanIds
        GROUP BY s.id
        """)
    List<Object[]> countWarningsByScanIds(@Param("scanIds") Collection<UUID> scanIds);

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