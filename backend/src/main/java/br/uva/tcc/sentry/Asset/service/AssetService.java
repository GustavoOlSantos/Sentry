package br.uva.tcc.sentry.Asset.service;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.Asset.DTO.AssetResponse;
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

    public List<AssetResponse> findAll(){
            return assetRepository.findAllWithServices().stream()
            .map(AssetResponse::from)
            .toList();
    }

    public AssetResponse findAById(UUID id){
            return assetRepository.findByIdWithServices(id).orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado: " + id));
    }

    public void deleteAById(UUID id){
            assetRepository.deleteById(id);
    }

    public Asset editById(Asset asset){
            return assetRepository.save(asset);
    }

}
