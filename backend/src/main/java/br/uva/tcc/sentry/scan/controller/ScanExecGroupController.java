package br.uva.tcc.sentry.scan.controller;

import br.uva.tcc.sentry.docs.api.ScanGroupApi;
import br.uva.tcc.sentry.scan.DTO.ScanGroupDetailResponse;
import br.uva.tcc.sentry.scan.DTO.ScanGroupSummaryResponse;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.services.ScanExecGroupService;
import br.uva.tcc.sentry.scan.services.ScanQueryService;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scanGroup")
public class ScanExecGroupController implements ScanGroupApi {

    private final ScanExecGroupService scanGroupService;
    private final ScanQueryService scanQueryService;

    public ScanExecGroupController(ScanExecGroupService scanGroupService, ScanQueryService scanQueryService) {
        this.scanGroupService = scanGroupService;
        this.scanQueryService = scanQueryService;
    }

    /** Grupos mais recentes, com status derivado, contagem de scans por status, ativos e findings. */
    @Override
    @GetMapping()
    public List<ScanGroupSummaryResponse> getAllScansGroups(){
       return scanQueryService.findGroups();
    }

    /** Um grupo e a lista de scans individuais dele. */
    @Override
    @GetMapping("/id/{id}")
    public ScanGroupDetailResponse getScanGroupByID(@PathVariable UUID id){
        return scanQueryService.findGroupDetail(id);
    }

    @Override
    @DeleteMapping("/id/{id}")
    public void deleteById(@PathVariable UUID id){
        scanGroupService.deleteById(id);
    }

    @Override
    @PutMapping("/{id}")
    public ScanExecutionGroup editById(@PathVariable UUID id, @RequestBody ScanExecutionGroup scan){
        return scanGroupService.editScan(scan);
    }
}