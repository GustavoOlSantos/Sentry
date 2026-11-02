package br.uva.tcc.sentry.finding.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import br.uva.tcc.sentry.finding.DTO.FindingBoardResponse;
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
}
