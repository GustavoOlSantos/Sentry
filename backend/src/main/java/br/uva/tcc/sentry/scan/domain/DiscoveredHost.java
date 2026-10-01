package br.uva.tcc.sentry.scan.domain;

import java.util.List;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.Service;

/**
 * Um host descoberto durante um scan: um {@link Asset} e os
 * {@link Service}s abertos encontrados nele.
 *
 * Existe porque o {@code Asset} não conhece seus serviços (quem guarda o FK é
 * o {@code Service}, como no diagrama). Fica em {@code scan.domain}
 * por ser o resultado de um scan (não é uma entidade persistida).
 */
public record DiscoveredHost(Asset asset, List<Service> services) {
}