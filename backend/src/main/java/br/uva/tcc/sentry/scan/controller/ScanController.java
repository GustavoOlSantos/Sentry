package br.uva.tcc.sentry.scan.controller;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.services.ScanService;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/scan")
public class ScanController{

   private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping()
    public ArrayList<Scan> getAllScans(){
       return scanService.findAll();
    }

    @GetMapping("/id/{id}")
    public Scan getSingleScanByID(@PathVariable UUID id){
        return scanService.findById(id);
    }

    @PostMapping("/exec")
    public ScanResult executeScan(){
       return scanService.executeScan("localhost");
    }

}