package br.uva.tcc.sentry.finding.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.repository.ScanRepository;
import br.uva.tcc.sentry.scan.services.ScanService;
import lombok.RequiredArgsConstructor;

/**
 * Atende o pedido de reteste: um reteste é só um scan comum do IP do asset.
 * Quem decide se o finding foi corrigido é o {@link FindingReconciler}, com as
 * mesmas regras de qualquer outro scan — não existe um caminho de validação
 * separado.
 */
@Component
@RequiredArgsConstructor
public class RetestRequestListener {

    private static final Logger log = LoggerFactory.getLogger(RetestRequestListener.class);

    private final ScanService scanService;
    private final ScanRepository scanRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRetestRequested(RetestRequestedEvent event) {
        String target = event.target();

        // um scan ainda PENDING começará depois da ação do usuário e já validará este finding;
        // evita N scans do mesmo host quando vários findings dele são resolvidos em sequência
        if (scanRepository.existsByTargetAndStatus(target, ScanStatus.PENDING)) {
            log.info("Reteste do finding {} dispensado: já há um scan pendente para {}.", event.findingId(), target);
            return;
        }

        try {
            scanService.executeScan(target);
            log.info("Reteste solicitado para {} (finding {}).", target, event.findingId());
        } catch (RuntimeException e) {
            // a ação do usuário já foi gravada; falhar o reteste não deve desfazê-la
            log.error("Não foi possível solicitar o reteste de {} (finding {}).", target, event.findingId(), e);
        }
    }
}
