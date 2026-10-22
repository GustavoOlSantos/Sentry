package br.uva.tcc.sentry.Asset.service;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service 
@RequiredArgsConstructor
public class AssetService {
    
    private final AssetRepository assetRepository;

    public List<Asset> findAll(){
            return assetRepository.findAll();
    }

    public Asset findAById(UUID id){
            return assetRepository.findById(id).orElseThrow();
    }

    public void deleteAById(UUID id){
            assetRepository.deleteById(id);
    }

    public Asset editById(Asset asset){
            return assetRepository.save(asset);
    }

}
