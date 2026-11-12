package br.uva.tcc.sentry.scan.controller;

import br.uva.tcc.sentry.scan.DTO.ScanRequest;
import br.uva.tcc.sentry.scan.DTO.ScanResponse;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.services.ScanService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/scan")
public class ScanController{

   private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping()
    public List<Scan> getAllScans(){
       return scanService.findAll();
    }

    @GetMapping("/id/{id}")
    public Optional<Scan> getSingleScanByID(@PathVariable UUID id){
        return scanService.findById(id);
    }

    @DeleteMapping("/id/{id}")
    public void deleteById(UUID id){
        scanService.deleteById(id);
    }

    @PutMapping("/{id}")
    public Scan editById(Scan scan){
        return scanService.editScan(scan);
    }

    @PostMapping("/exec")
    public ResponseEntity<?> executeScan(@RequestBody ScanRequest request){
        if (request.targets() == null || request.targets().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "O campo 'targets' é obrigatório e não pode estar vazio.")
            );
        }
        
       ScanResponse response = scanService.executeScan(request.targets());
       
       if(response.status() == ScanStatus.FAILED){
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
       }
       else{
        return ResponseEntity.accepted().body(response);
       }
    }

}