package br.uva.tcc.sentry.finding.checker;

import java.util.List;

import br.uva.tcc.sentry.finding.domain.ConfigFinding;
import br.uva.tcc.sentry.finding.domain.Host;

/**
 * Uma checagem de configuração insegura avaliável diretamente a partir do
 * que o Nmap já encontrou em um host (não depende de consulta a CVE/NVD).
 *
 * Cada checagem concreta tem uma única responsabilidade (porta de banco
 * exposta, SSH exposto, certificado expirado, etc). Adicionar uma nova
 * checagem é implementar esta interface e anotar com {@code @Component} —
 * o {@link ConfigFindingService} descobre e executa todas automaticamente,
 * sem precisar ser alterado.
 */
public interface ConfigCheck {

    List<ConfigFinding> check(Host host);
}
