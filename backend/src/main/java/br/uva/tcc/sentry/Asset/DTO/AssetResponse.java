package br.uva.tcc.sentry.Asset.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;

public record AssetResponse(
    UUID id,
    String ipAddress,
    String hostname,
    String macAddress,
    String operatingSystem,
    AssetStatus status,
    LocalDateTime firstSeenAt,
    LocalDateTime lastSeenAt,
    List<ServiceResponse> services
    
) {
    public static AssetResponse from(Asset asset) {
        return new AssetResponse(
            asset.getId(),
            asset.getIpAddress(),
            asset.getHostname(),
            asset.getMacAddress(),
            asset.getOperatingSystem(),
            asset.getStatus(),
            asset.getFirstSeenAt(),
            asset.getLastSeenAt(),
            asset.getServices()
                .stream()
                .map(ServiceResponse::from)
                .toList()
        );
    }
}