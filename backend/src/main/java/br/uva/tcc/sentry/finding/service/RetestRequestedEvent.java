package br.uva.tcc.sentry.finding.service;

import java.util.UUID;

/**
 * Publicado quando o usuário marca um finding como RESOLVED: é o pedido de
 * reteste. Só é atendido depois do commit (ver {@link RetestRequestListener}),
 * para que o scan sempre enxergue o status já gravado.
 */
public record RetestRequestedEvent(UUID findingId, String target) {
}
