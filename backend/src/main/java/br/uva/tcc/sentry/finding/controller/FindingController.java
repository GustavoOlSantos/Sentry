package br.uva.tcc.sentry.finding.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.uva.tcc.sentry.finding.DTO.FindingBoardResponse;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingAssigneeRequest;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingStatusRequest;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingStatusResponse;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.service.FindingService;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor
@RequestMapping("/findings")
public class FindingController {
    private final FindingService findingService;

    @GetMapping("")
    public List<FindingBoardResponse> findAll(){
        return findingService.findAll();
    }

    @PostMapping("/{findingId}/status")
    public ResponseEntity<?> updateFindingStatus(@PathVariable UUID findingId, @RequestBody UpdateFindingStatusRequest request) {
        FindingStatus newStatus = request.status();
        String justification = request.justification();

        if (newStatus == null)  return ResponseEntity.badRequest().body("O campo 'status' é obrigatório.");
        if(findingId == null) return ResponseEntity.badRequest().body("O campo 'findingId' é obrigatório.");
        if(newStatus == FindingStatus.JUSTIFIED && (justification == null || justification.trim().isEmpty())) return ResponseEntity.badRequest().body("O campo 'justification' é obrigatório quando o status é JUSTIFIED.");
        
        UpdateFindingStatusResponse response = findingService.updateFindingStatus(findingId, newStatus, justification);

        if (response.message() == "Finding não encontrado.") {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{findingId}/assignee")
    public ResponseEntity<?> updateFindingStatus(@PathVariable UUID findingId, @RequestBody UpdateFindingAssigneeRequest request) {
        var assigneeId = request.assigneeId();

        if(findingId == null) return ResponseEntity.badRequest().body("O campo 'findingId' é obrigatório.");
        if(assigneeId == null) return ResponseEntity.badRequest().body("O campo 'assigneeId' é obrigatório.");

        var response = findingService.updateFindingAssignee(findingId, assigneeId);

        return ResponseEntity.ok(response);
    }
}
