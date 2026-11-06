package br.uva.tcc.sentry.policy.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.policy.domain.PolicyStatus;
import br.uva.tcc.sentry.policy.repository.PolicyRuleRepository;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;

/**
 * Checagem de configuração dinâmica: carrega as {@link PolicyRule} ativas
 * cadastradas no banco e avalia cada uma contra os serviços descobertos
 * no host, gerando um {@link Finding} por violação.
 *
 * Diferente dos checkers fixos ({@code ExposedSshCheck} e companhia), as
 * regras aqui não são hardcoded — vêm do banco e podem ser criadas/
 * editadas/desabilitadas pelo usuário via {@code PolicyRepository}/
 * {@code PolicyRuleRepository}, sem precisar alterar nenhum código.
 *
 * Implementa {@link ConfigCheck} como qualquer outro checker: o Spring
 * injeta este bean automaticamente na lista usada pelo
 * {@code ConfigFindingService}, e os findings resultantes seguem o mesmo
 * caminho de persistência que os demais — por isso já aparecem no board
 * de findings e contam nos indicadores de cada asset, sem precisar de
 * nenhum código adicional de integração.
 *
 * Tipos de regra suportados hoje ({@link br.uva.tcc.sentry.policy.domain.PolicyRuleType}):
 * <ul>
 *   <li>{@code PORT_RESTRICTION}: {@code expectedValue} é o número da porta
 *       que não deveria estar aberta (ex.: "23"). {@code condition},
 *       opcional, restringe por protocolo (ex.: "tcp"); em branco, vale
 *       pra qualquer protocolo.</li>
 *   <li>{@code FORBIDDEN_SERVICE}: {@code expectedValue} é um trecho do
 *       nome do serviço/produto que não deveria estar rodando (ex.:
 *       "telnet"), comparado sem diferenciar maiúsculas/minúsculas contra
 *       o nome do serviço e o produto identificados pelo Nmap.</li>
 * </ul>
 *
 * Esses dois formatos são uma primeira definição razoável pro que a
 * política consegue expressar — se o time decidir por um formato
 * diferente (ex.: JSON estruturado em vez de string simples), é só
 * ajustar os dois métodos {@code evaluate*} abaixo; o resto do fluxo não
 * muda.
 */
@Component
public class PolicyRuleEvaluator implements ConfigCheck {

    private final PolicyRuleRepository policyRuleRepository;
    private final FindingFactory findingFactory;

    public PolicyRuleEvaluator(PolicyRuleRepository policyRuleRepository, FindingFactory findingFactory) {
        this.policyRuleRepository = policyRuleRepository;
        this.findingFactory = findingFactory;
    }

    @Override
    public List<Finding> check(DiscoveredHost host) {
        List<PolicyRule> activeRules = policyRuleRepository.findActiveEnabledRules(PolicyStatus.ACTIVE);

        if (activeRules.isEmpty()) {
            return List.of();
        }

        List<Finding> findings = new ArrayList<>();

        for (PolicyRule rule : activeRules) {
            for (Service service : host.services()) {
                if (!"open".equalsIgnoreCase(service.getState())) {
                    continue;
                }

                evaluate(rule, service).ifPresent(evidence ->
                        findings.add(findingFactory.fromPolicyViolation(host, service, rule, evidence, null)));
            }
        }

        return findings;
    }

    private Optional<String> evaluate(PolicyRule rule, Service service) {
        if (rule.getType() == null) {
            return Optional.empty();
        }

        return switch (rule.getType()) {
            case PORT_RESTRICTION -> evaluatePortRestriction(rule, service);
            case FORBIDDEN_SERVICE -> evaluateForbiddenService(rule, service);
        };
    }

    private Optional<String> evaluatePortRestriction(PolicyRule rule, Service service) {
        Integer forbiddenPort = parsePort(rule.getExpectedValue());

        if (forbiddenPort == null || !forbiddenPort.equals(service.getPort())) {
            return Optional.empty();
        }

        String expectedProtocol = rule.getCondition();
        if (expectedProtocol != null && !expectedProtocol.isBlank()
                && !expectedProtocol.equalsIgnoreCase(service.getProtocol())) {
            return Optional.empty();
        }

        return Optional.of("Porta restrita pela política \"" + rule.getPolicy().getName() + "\": "
                + service.getPort() + "/" + service.getProtocol() + " está aberta.");
    }

    private Optional<String> evaluateForbiddenService(PolicyRule rule, Service service) {
        String forbidden = rule.getExpectedValue();
        if (forbidden == null || forbidden.isBlank()) {
            return Optional.empty();
        }

        boolean matches = containsIgnoreCase(service.getServiceName(), forbidden)
                || containsIgnoreCase(service.getProduct(), forbidden);

        if (!matches) {
            return Optional.empty();
        }

        return Optional.of("Serviço proibido pela política \"" + rule.getPolicy().getName() + "\": "
                + describeService(service) + " na porta " + service.getPort() + "/" + service.getProtocol());
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private String describeService(Service service) {
        if (service.getProduct() != null && !service.getProduct().isBlank()) {
            return service.getProduct();
        }
        return service.getServiceName() != null ? service.getServiceName() : "desconhecido";
    }

    private Integer parsePort(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
