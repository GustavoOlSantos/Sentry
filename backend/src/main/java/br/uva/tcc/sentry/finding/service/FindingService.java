package br.uva.tcc.sentry.finding.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.finding.DTO.FindingBoardResponse;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingAssigneeResponse;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingStatusResponse;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.repository.FindingRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class FindingService {

    private final FindingRepository findingRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional(readOnly = true)
    public List<FindingBoardResponse> findAll() {
        return findingRepository.findAll()
            .stream()
            .map(FindingBoardResponse::from)
            .toList();
    }

    /**
     * Mudança de status feita por uma pessoa. As regras (quais status o usuário
     * pode definir, o que cada um limpa/preenche) ficam em
     * {@link Finding#changeStatusByUser}. Marcar como RESOLVED também pede um
     * reteste, que roda depois do commit.
     */
    @Transactional
    public UpdateFindingStatusResponse updateFindingStatus(UUID findingId, FindingStatus newStatus, String justification) {
        Finding finding = findById(findingId);
        FindingStatus previousStatus = finding.getStatus();

        finding.changeStatusByUser(newStatus, justification, LocalDateTime.now());
        findingRepository.save(finding);

        String message = "Status do finding atualizado com sucesso.";

        if (newStatus == FindingStatus.RESOLVED) {
            eventPublisher.publishEvent(new RetestRequestedEvent(finding.getId(), finding.getAsset().getIpAddress()));
            message += " Um reteste foi solicitado para validar a correção.";
        }

        return new UpdateFindingStatusResponse(findingId, previousStatus, newStatus, message);
    }

    @Transactional
    public UpdateFindingAssigneeResponse updateFindingAssignee(UUID findingId, Long assigneeId) {
        Finding finding = findById(findingId);

        finding.setAssigneeId(assigneeId);
        findingRepository.save(finding);

        return new UpdateFindingAssigneeResponse(assigneeId, findingId, "Assignee do finding atualizado com sucesso.");
    }

    private Finding findById(UUID findingId) {
        return findingRepository.findById(findingId)
            .orElseThrow(() -> new EntityNotFoundException("Finding não encontrado."));
    }
}
