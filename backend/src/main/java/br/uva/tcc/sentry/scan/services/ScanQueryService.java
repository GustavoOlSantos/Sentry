package br.uva.tcc.sentry.scan.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.repository.ServiceRepository;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.repository.FindingRepository;
import br.uva.tcc.sentry.scan.DTO.ScanDetailResponse;
import br.uva.tcc.sentry.scan.DTO.ScanDetailResponse.OpenPortResponse;
import br.uva.tcc.sentry.scan.DTO.ScanDetailResponse.ScanAssetResponse;
import br.uva.tcc.sentry.scan.DTO.ScanDetailResponse.ScanFindingResponse;
import br.uva.tcc.sentry.scan.DTO.ScanGroupDetailResponse;
import br.uva.tcc.sentry.scan.DTO.ScanGroupSummaryResponse;
import br.uva.tcc.sentry.scan.DTO.ScanSummaryResponse;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanAsset;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.repository.ScanAssetRepository;
import br.uva.tcc.sentry.scan.repository.ScanExecutionGroupRepository;
import br.uva.tcc.sentry.scan.repository.ScanRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Consultas de leitura para as telas de varreduras (grupos e scans individuais).
 *
 * Devolve somente DTOs: as entidades {@code Scan} e {@code ScanExecutionGroup}
 * se referenciam em ciclo (grupo → scans → grupo) e não podem ser serializadas
 * diretamente. Os totais vêm de consultas agrupadas (uma por tipo de número,
 * não uma por linha), então listar N grupos custa um número fixo de queries.
 */
@Service
@RequiredArgsConstructor
public class ScanQueryService {

    private final ScanExecutionGroupRepository groupRepository;
    private final ScanRepository scanRepository;
    private final ScanAssetRepository scanAssetRepository;
    private final FindingRepository findingRepository;
    private final ServiceRepository serviceRepository;

    // ------------------------------------------------------------------
    // Grupos
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ScanGroupSummaryResponse> findGroups() {
        List<ScanExecutionGroup> groups = groupRepository.findTop200ByOrderByCreatedAtDesc();
        if (groups.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = groups.stream().map(ScanExecutionGroup::getId).toList();

        Map<UUID, Map<ScanStatus, Long>> statusCounts = statusCountsByGroup(ids);
        Map<UUID, List<String>> targets = targetsByGroup(ids);
        Map<UUID, Long> assets = longMap(scanAssetRepository.countDistinctAssetsByGroupIds(ids));
        Map<UUID, Long> findings = longMap(findingRepository.countByGroupIds(ids));

        return groups.stream()
                .map(group -> toGroupSummary(group,
                        statusCounts.getOrDefault(group.getId(), new EnumMap<>(ScanStatus.class)),
                        targets.getOrDefault(group.getId(), List.of()),
                        assets.getOrDefault(group.getId(), 0L),
                        findings.getOrDefault(group.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ScanGroupDetailResponse findGroupDetail(UUID groupId) {
        ScanExecutionGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo de scan não encontrado."));

        List<Scan> scans = scanRepository.findByExecGroupIdOrderByCreatedAtAsc(groupId);
        List<UUID> groupIds = List.of(groupId);

        Map<UUID, Map<ScanStatus, Long>> statusCounts = statusCountsByGroup(groupIds);
        Map<UUID, Long> assets = longMap(scanAssetRepository.countDistinctAssetsByGroupIds(groupIds));
        Map<UUID, Long> findings = longMap(findingRepository.countByGroupIds(groupIds));

        ScanGroupSummaryResponse summary = toGroupSummary(group,
                statusCounts.getOrDefault(groupId, new EnumMap<>(ScanStatus.class)),
                scans.stream().map(Scan::getTarget).toList(),
                assets.getOrDefault(groupId, 0L),
                findings.getOrDefault(groupId, 0L));

        return new ScanGroupDetailResponse(summary, toScanSummaries(scans));
    }

    // ------------------------------------------------------------------
    // Scans individuais
    // ------------------------------------------------------------------

    /** Os 200 scans mais recentes, de qualquer grupo. */
    @Transactional(readOnly = true)
    public List<ScanSummaryResponse> findRecentScans() {
        return toScanSummaries(scanRepository.findTop200ByOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public ScanDetailResponse findScanDetail(UUID scanId) {
        Scan scan = scanRepository.findWithExecGroupById(scanId)
                .orElseThrow(() -> new EntityNotFoundException("Scan não encontrado."));

        List<ScanAssetResponse> assets = scanAssetRepository.findWithAssetByScanId(scanId).stream()
                .map(this::toScanAsset)
                .toList();

        List<ScanFindingResponse> findings = findingRepository.findWithAssetByScanId(scanId).stream()
                .map(this::toScanFinding)
                .toList();

        return new ScanDetailResponse(
                scan.getId(),
                scan.getExecGroup() != null ? scan.getExecGroup().getId() : null,
                scan.getTarget(),
                scan.getCommand(),
                scan.getStatus(),
                scan.getErrorMessage(),
                new ArrayList<>(scan.getWarnings()),
                scan.getCreatedAt(),
                scan.getStartedAt(),
                scan.getFinishedAt(),
                durationSeconds(scan.getStartedAt(), scan.getFinishedAt()),
                assets,
                findings);
    }

    // ------------------------------------------------------------------
    // Montagem dos DTOs
    // ------------------------------------------------------------------

    private ScanGroupSummaryResponse toGroupSummary(ScanExecutionGroup group, Map<ScanStatus, Long> scansByStatus,
                                                    List<String> targets, long assetsCount, long findingsCount) {
        int totalScans = scansByStatus.values().stream().mapToInt(Long::intValue).sum();

        return new ScanGroupSummaryResponse(
                group.getId(),
                ScanGroupStatusResolver.resolve(group.getStatus(), scansByStatus),
                group.getCreatedAt(),
                group.getStartedAt(),
                group.getFinishedAt(),
                durationSeconds(group.getStartedAt(), group.getFinishedAt()),
                group.getErrorMessage(),
                totalScans,
                scansByStatus,
                targets,
                assetsCount,
                findingsCount);
    }

    private List<ScanSummaryResponse> toScanSummaries(List<Scan> scans) {
        if (scans.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = scans.stream().map(Scan::getId).toList();
        Map<UUID, Long> assets = longMap(scanAssetRepository.countByScanIds(ids));
        Map<UUID, Long> findings = longMap(findingRepository.countByScanIds(ids));
        Map<UUID, Long> warnings = longMap(scanRepository.countWarningsByScanIds(ids));

        return scans.stream()
                .map(scan -> new ScanSummaryResponse(
                        scan.getId(),
                        scan.getExecGroup() != null ? scan.getExecGroup().getId() : null,
                        scan.getTarget(),
                        scan.getStatus(),
                        scan.getCreatedAt(),
                        scan.getStartedAt(),
                        scan.getFinishedAt(),
                        durationSeconds(scan.getStartedAt(), scan.getFinishedAt()),
                        scan.getErrorMessage(),
                        assets.getOrDefault(scan.getId(), 0L),
                        findings.getOrDefault(scan.getId(), 0L),
                        warnings.getOrDefault(scan.getId(), 0L)))
                .toList();
    }

    private ScanAssetResponse toScanAsset(ScanAsset scanAsset) {
        Asset asset = scanAsset.getAsset();

        List<OpenPortResponse> openPorts = serviceRepository.findByAssetId(asset.getId()).stream()
                .filter(br.uva.tcc.sentry.Asset.domain.Service::isOpen)
                .sorted((a, b) -> Integer.compare(a.getPort(), b.getPort()))
                .map(service -> new OpenPortResponse(
                        service.getPort(), service.getProtocol(), service.getServiceName(),
                        service.getProduct(), service.getVersion()))
                .toList();

        return new ScanAssetResponse(
                asset.getId(),
                asset.getIpAddress(),
                asset.getHostname(),
                asset.getOperatingSystem(),
                asset.getStatus(),
                scanAsset.getDiscoveredAt(),
                openPorts);
    }

    private ScanFindingResponse toScanFinding(Finding finding) {
        var service = finding.getService();

        return new ScanFindingResponse(
                finding.getId(),
                finding.getType(),
                finding.getCode(),
                finding.getTitle(),
                finding.getSeverity(),
                finding.getStatus(),
                finding.getAsset().getId(),
                finding.getAsset().getIpAddress(),
                service != null ? service.getPort() : null,
                service != null ? service.getProtocol() : null);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Map<UUID, Map<ScanStatus, Long>> statusCountsByGroup(Collection<UUID> groupIds) {
        Map<UUID, Map<ScanStatus, Long>> result = new HashMap<>();

        for (Object[] row : scanRepository.countByGroupAndStatus(groupIds)) {
            result.computeIfAbsent((UUID) row[0], id -> new EnumMap<>(ScanStatus.class))
                    .put((ScanStatus) row[1], ((Number) row[2]).longValue());
        }
        return result;
    }

    private Map<UUID, List<String>> targetsByGroup(Collection<UUID> groupIds) {
        Map<UUID, List<String>> result = new LinkedHashMap<>();

        for (Object[] row : scanRepository.findTargetsByGroupIds(groupIds)) {
            result.computeIfAbsent((UUID) row[0], id -> new ArrayList<>()).add((String) row[1]);
        }
        return result;
    }

    /** Linhas [id, quantidade] → mapa id → quantidade. */
    private Map<UUID, Long> longMap(List<Object[]> rows) {
        Map<UUID, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private Long durationSeconds(LocalDateTime start, LocalDateTime end) {
        return (start != null && end != null) ? Duration.between(start, end).getSeconds() : null;
    }
}
