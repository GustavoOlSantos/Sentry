package br.uva.tcc.sentry.Asset.DTO;

import br.uva.tcc.sentry.Asset.domain.Service;
import java.util.UUID;

public record ServiceResponse(
    UUID id,
    Integer port,
    String state,
    String protocol,
    String product,
    String serviceName,
    String version,
    String extraInfo,
    String cpe
) {
    public static ServiceResponse from(Service service) {
        return new ServiceResponse(
            service.getId(),
            service.getPort(),
            service.getState(),
            service.getProtocol(),
            service.getProduct(),
            service.getServiceName(),
            service.getVersion(),
            service.getExtraInfo(),
            service.getCpe()
        );
    }
}