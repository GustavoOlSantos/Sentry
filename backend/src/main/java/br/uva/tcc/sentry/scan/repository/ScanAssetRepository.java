package br.uva.tcc.sentry.scan.repository;

import br.uva.tcc.sentry.scan.domain.ScanAsset;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanAssetRepository extends JpaRepository<ScanAsset, Long> {

    List<ScanAsset> findByScanId(UUID scanId);

    List<ScanAsset> findByAssetId(UUID assetId);
}