package br.uva.tcc.sentry.Asset.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.service.AssetService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController 
@RequiredArgsConstructor
@RequestMapping("/asset")
public class AssetController {
    
    private final AssetService assetService;

    @GetMapping("")
    public List<Asset> findAll(){
        return assetService.findAll();
    }

    @GetMapping("/{id}")
    public Asset findAById(UUID id){
        return assetService.findAById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteAById(UUID id){
        assetService.deleteAById(id);
    }

    @PutMapping("/{id}")
    public Asset editById(Asset asset){
        return assetService.editById(asset);
    }
}
