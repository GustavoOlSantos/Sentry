package br.uva.tcc.sentry.Asset.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import br.uva.tcc.sentry.Asset.domain.AssetStatus;

public record AssetSummaryResponse(
    UUID id,
    String ipAddress,
    String hostname,
    String macAddress,
    String operatingSystem,
    AssetStatus status,
    LocalDateTime firstSeenAt,
    LocalDateTime lastSeenAt,
    Integer servicesCount,
    Integer findingsCount
) {}