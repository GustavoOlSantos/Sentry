package br.uva.tcc.sentry.Asset.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uva.tcc.sentry.docs.api.AssetApi;
import br.uva.tcc.sentry.Asset.DTO.AssetResponse;
import br.uva.tcc.sentry.Asset.DTO.AssetSummaryResponse;
import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.service.AssetService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController 
@RequiredArgsConstructor
@RequestMapping("/asset")
public class AssetController implements AssetApi {
    
    private final AssetService assetService;

    @Override
    @GetMapping("")
    public List<AssetResponse> findAll(){
        return assetService.findAll();
    }

    @Override
    @GetMapping("/summary")
    public List<AssetSummaryResponse> findAllAssetSummary(){
        return assetService.findAllAssetSummary();
    }

    @Override
    @GetMapping("/{id}")
    public AssetResponse findAById(@PathVariable UUID id){
        return assetService.findById(id);
    }

    @Override
    @DeleteMapping("/{id}")
    public void deleteAById(@PathVariable UUID id){
        assetService.deleteAById(id);
    }

    @Override
    @PutMapping("/{id}")
    public Asset editById(@PathVariable UUID id, @RequestBody Asset asset){
        return assetService.editById(asset);
    }
}
