package br.uva.tcc.sentry.Asset.repository;

import br.uva.tcc.sentry.Asset.domain.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByAssetId(UUID assetId);

    Optional<Service> findByAssetIdAndPortAndProtocol(UUID assetId, Integer port, String protocol);
}
