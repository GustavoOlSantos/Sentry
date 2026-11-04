package br.uva.tcc.sentry.finding.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.finding.DTO.FindingBoardResponse;
import br.uva.tcc.sentry.finding.DTO.UpdateFindingStatusResponse;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.repository.FindingRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class FindingService {

    private final FindingRepository findingRepository;
    
    @Transactional(readOnly = true)
    public List<FindingBoardResponse> findAll() {
        return findingRepository.findAll()
            .stream()
            .map(FindingBoardResponse::from)
            .toList();
    }

    public UpdateFindingStatusResponse updateFindingStatus(UUID findingId, FindingStatus newStatus) {
        var finding = findingRepository.findById(findingId).orElse(null);
        
        if (finding == null) {
            return new UpdateFindingStatusResponse(findingId, null, newStatus, "Finding não encontrado.");
        }
            
        FindingStatus previousStatus = finding.getStatus();
        finding.setStatus(newStatus);
        findingRepository.save(finding);

        return new UpdateFindingStatusResponse(findingId, previousStatus, newStatus, "Status do finding atualizado com sucesso.");
    }
}
