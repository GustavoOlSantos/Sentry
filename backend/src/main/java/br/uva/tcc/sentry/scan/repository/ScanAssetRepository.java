package br.uva.tcc.sentry.scan.repository;

import br.uva.tcc.sentry.scan.domain.ScanAsset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScanAssetRepository extends JpaRepository<ScanAsset, Long> {

    List<ScanAsset> findByScanId(UUID scanId);

    List<ScanAsset> findByAssetId(UUID assetId);

    boolean existsByScanIdAndAssetId(UUID scanId, UUID assetId);

    /** [groupId, ativos distintos] encontrados pelos scans de cada grupo. */
    @Query("""
        SELECT sa.scan.execGroup.id, COUNT(DISTINCT sa.asset.id)
        FROM ScanAsset sa
        WHERE sa.scan.execGroup.id IN :groupIds
        GROUP BY sa.scan.execGroup.id
        """)
    List<Object[]> countDistinctAssetsByGroupIds(@Param("groupIds") Collection<UUID> groupIds);

    /** [scanId, ativos encontrados]. */
    @Query("""
        SELECT sa.scan.id, COUNT(sa)
        FROM ScanAsset sa
        WHERE sa.scan.id IN :scanIds
        GROUP BY sa.scan.id
        """)
    List<Object[]> countByScanIds(@Param("scanIds") Collection<UUID> scanIds);

    @Query("""
        SELECT sa FROM ScanAsset sa
        JOIN FETCH sa.asset a
        WHERE sa.scan.id = :scanId
        ORDER BY a.ipAddress
        """)
    List<ScanAsset> findWithAssetByScanId(@Param("scanId") UUID scanId);
}