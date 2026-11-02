package br.uva.tcc.sentry.Asset.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uva.tcc.sentry.Asset.DTO.AssetResponse;
import br.uva.tcc.sentry.Asset.DTO.AssetSummaryResponse;
import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.repository.AssetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service 
@RequiredArgsConstructor
public class AssetService {
    
        private final AssetRepository assetRepository;

        @Transactional(readOnly = true)
        public List<AssetResponse> findAll() {
                var assets = assetRepository.findAllWithServices();

                assetRepository.findAllWithFindings();

                return assets.stream()
                .map(AssetResponse::from)
                .toList();
        }

        public List<AssetSummaryResponse> findAllAssetSummary() {
            return assetRepository.findAllAssetSummary();
        }

        @Transactional(readOnly = true)
        public AssetResponse findById(UUID id) {
                var asset = assetRepository.findByIdWithServices(id)
                .orElseThrow(() -> new EntityNotFoundException("Asset não encontrado"));

                assetRepository.findByIdWithFindings(id);

                return AssetResponse.from(asset);
        }

    public void deleteAById(UUID id){
        assetRepository.deleteById(id);
    }

    public Asset editById(Asset asset){
        return assetRepository.save(asset);
    }

}
