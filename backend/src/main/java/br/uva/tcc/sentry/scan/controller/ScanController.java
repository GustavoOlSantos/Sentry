package br.uva.tcc.sentry.scan.controller;

import br.uva.tcc.sentry.scan.DTO.ScanDetailResponse;
import br.uva.tcc.sentry.scan.DTO.ScanRequest;
import br.uva.tcc.sentry.scan.DTO.ScanResponse;
import br.uva.tcc.sentry.scan.DTO.ScanSummaryResponse;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.services.ScanQueryService;
import br.uva.tcc.sentry.scan.services.ScanService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/scan")
public class ScanController{

   private final ScanService scanService;
   private final ScanQueryService scanQueryService;

    public ScanController(ScanService scanService, ScanQueryService scanQueryService) {
        this.scanService = scanService;
        this.scanQueryService = scanQueryService;
    }

    /** Os scans mais recentes (de qualquer grupo), em formato resumido. */
    @GetMapping()
    public List<ScanSummaryResponse> getAllScans(){
       return scanQueryService.findRecentScans();
    }

    /** Detalhes de um scan: comando, avisos, ativos encontrados e findings detectados. */
    @GetMapping("/id/{id}")
    public ScanDetailResponse getSingleScanByID(@PathVariable UUID id){
        return scanQueryService.findScanDetail(id);
    }

    @DeleteMapping("/id/{id}")
    public void deleteById(@PathVariable UUID id){
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