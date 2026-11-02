package br.uva.tcc.sentry.Asset.DTO;

import java.util.UUID;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;

public record AssetReferenceResponse(
    UUID id,
    String ipAddress,
    String hostname,
    String macAddress,
    String operatingSystem,
    AssetStatus status
) {
    public static AssetReferenceResponse from(Asset asset) {
        return new AssetReferenceResponse(
            asset.getId(),
            asset.getIpAddress(),
            asset.getHostname(),
            asset.getMacAddress(),
            asset.getOperatingSystem(),
            asset.getStatus()
        );
    }
}
