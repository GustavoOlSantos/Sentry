package br.uva.tcc.sentry.scan.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.Asset.domain.AssetStatus;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.shared.domain.Severity;

/** Tudo sobre um scan individual: execução, ativos encontrados e findings detectados por ele. */
public record ScanDetailResponse(
    UUID id,
    UUID groupId,
    String target,
    /** Comando Nmap executado (preenchido quando o scan chega a rodar). */
    String command,
    ScanStatus status,
    String errorMessage,
    List<String> warnings,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Long durationSeconds,
    List<ScanAssetResponse> assets,
    List<ScanFindingResponse> findings
) {

    public record ScanAssetResponse(
        UUID id,
        String ipAddress,
        String hostname,
        String operatingSystem,
        AssetStatus status,
        LocalDateTime discoveredAt,
        List<OpenPortResponse> openPorts
    ) {
    }

    public record OpenPortResponse(
        Integer port,
        String protocol,
        String service,
        String product,
        String version
    ) {
    }

    /** Findings que este scan detectou pela primeira vez (reencontros de findings antigos não entram). */
    public record ScanFindingResponse(
        UUID id,
        FindingType type,
        String code,
        String title,
        Severity severity,
        FindingStatus status,
        UUID assetId,
        String assetIp,
        Integer port,
        String protocol
    ) {
    }
}
