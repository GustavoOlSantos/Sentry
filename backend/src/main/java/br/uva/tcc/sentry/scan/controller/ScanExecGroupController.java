package br.uva.tcc.sentry.scan.controller;

import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.services.ScanExecGroupService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scanGroup")
public class ScanExecGroupController{

   private final ScanExecGroupService scanGroupService;

    public ScanExecGroupController(ScanExecGroupService scanGroupService) {
        this.scanGroupService = scanGroupService;
    }

    @GetMapping()
    public List<ScanExecutionGroup> getAllScansGroups(){
       return scanGroupService.findAll();
    }

    @GetMapping("/id/{id}")
    public Optional<ScanExecutionGroup> getScanGroupByID(@PathVariable UUID id){
        return scanGroupService.findById(id);
    }

    @DeleteMapping("/id/{id}")
    public void deleteById(UUID id){
        scanGroupService.deleteById(id);
    }

    @PutMapping("/{id}")
    public ScanExecutionGroup editById(ScanExecutionGroup scan){
        return scanGroupService.editScan(scan);
    }
}