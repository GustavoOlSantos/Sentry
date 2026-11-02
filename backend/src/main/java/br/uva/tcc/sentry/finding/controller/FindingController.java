package br.uva.tcc.sentry.finding.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.uva.tcc.sentry.finding.DTO.FindingBoardResponse;
import br.uva.tcc.sentry.finding.service.FindingService;
import java.util.List;
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
}
