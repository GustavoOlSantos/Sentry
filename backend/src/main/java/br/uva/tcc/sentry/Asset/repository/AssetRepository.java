package br.uva.tcc.sentry.Asset.repository;

import br.uva.tcc.sentry.Asset.domain.Asset;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;



public interface AssetRepository extends JpaRepository<Asset, UUID> {
    
}
