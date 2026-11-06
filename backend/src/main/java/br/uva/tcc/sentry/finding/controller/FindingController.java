package br.uva.tcc.sentry.finding.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.uva.tcc.sentry.finding.DTO.FindingBoardResponse;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingAssigneeRequest;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingStatusRequest;
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

        if (newStatus == null) return ResponseEntity.badRequest().body("O campo 'status' é obrigatório.");
        if (newStatus == FindingStatus.VALIDATED) return ResponseEntity.badRequest().body("A alteração de status para 'Válido' é permitido apenas pela própria ferramenta.");
        if (newStatus == FindingStatus.FAILED_VALIDATION) return ResponseEntity.badRequest().body("A alteração de status para 'Inválido' é permitido apenas pela própria ferramenta.");
        if (newStatus == FindingStatus.JUSTIFIED && (justification == null || justification.trim().isEmpty())) return ResponseEntity.badRequest().body("O campo 'justification' é obrigatório quando o status é JUSTIFIED.");

        // finding inexistente → 404, regra de transição violada → 409 (ver GlobalExceptionHandler)
        return ResponseEntity.ok(findingService.updateFindingStatus(findingId, newStatus, justification));
    }

    @PatchMapping("/{findingId}/assignee")
    public ResponseEntity<?> updateFindingAssignee(@PathVariable UUID findingId, @RequestBody UpdateFindingAssigneeRequest request) {
        var assigneeId = request.assigneeId();

        if(assigneeId == null) return ResponseEntity.badRequest().body("O campo 'assigneeId' é obrigatório.");

        return ResponseEntity.ok(findingService.updateFindingAssignee(findingId, assigneeId));
    }
}
