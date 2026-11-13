package br.uva.tcc.sentry.scan.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.Asset.repository.ServiceRepository;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.finding.repository.FindingRepository;
import br.uva.tcc.sentry.scan.DTO.ScanDetailResponse;
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
import br.uva.tcc.sentry.shared.domain.Severity;
import jakarta.persistence.EntityNotFoundException;

class ScanQueryServiceTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 10, 9, 0);

    private final List<ScanExecutionGroup> groups = new ArrayList<>();
    private final List<Scan> scans = new ArrayList<>();
    private final List<ScanAsset> scanAssets = new ArrayList<>();
    private final List<Finding> findings = new ArrayList<>();
    private final List<Service> services = new ArrayList<>();

    private ScanQueryService queryService;

    private ScanExecutionGroup oldGroup;   // 1 COMPLETED + 1 FAILED  → PARTIAL
    private ScanExecutionGroup newGroup;   // gravado como COMPLETED, mas um scan ainda RUNNING
    private Scan okScan;
    private Scan failedScan;
    private Asset web;
    private Asset db;

    @BeforeEach
    void setUp() {
        queryService = new ScanQueryService(groupRepo(), scanRepo(), scanAssetRepo(), findingRepo(), serviceRepo());

        oldGroup = group(T0);
        newGroup = group(T0.plusHours(1));
        newGroup.setStatus(ScanStatus.COMPLETED); // o que o verificaGrupo grava por engano

        okScan = scan("10.0.0.5", oldGroup, ScanStatus.COMPLETED, T0.plusMinutes(1), T0.plusMinutes(4));
        okScan.setCommand("nmap -sV -O 10.0.0.5");
        okScan.getWarnings().add("OS detection limitada");
        failedScan = scan("10.0.0.9", oldGroup, ScanStatus.FAILED, T0.plusMinutes(1), T0.plusMinutes(2));
        failedScan.setErrorMessage("Timeout do Nmap");
        scan("10.0.1.0/24", newGroup, ScanStatus.RUNNING, T0.plusHours(1), null);
        oldGroup.setStartedAt(T0.plusMinutes(1));
        oldGroup.setFinishedAt(T0.plusMinutes(5));

        web = asset("10.0.0.5", "web-01");
        db = asset("10.0.0.6", null);
        scanAssets.add(new ScanAsset(okScan, web, T0.plusMinutes(2)));
        scanAssets.add(new ScanAsset(okScan, db, T0.plusMinutes(3)));

        Service http = service(web, 80, "http", true);
        Service ssh = service(web, 22, "ssh", true);
        service(web, 8080, "http-alt", false); // fechada: não aparece
        Service mysql = service(db, 3306, "mysql", true);

        findings.add(finding(web, http, "CVE-2021-0001", Severity.CRITICAL, okScan));
        findings.add(finding(db, mysql, "EXPOSED_DATABASE_PORT", Severity.HIGH, okScan));
        findings.add(finding(web, ssh, "SSH_EXPOSED", Severity.MEDIUM, null)); // veio de outro scan
    }

    // ---------------- grupos ----------------

    @Test
    void findGroups_newestFirst_withDerivedStatusAndTotals() {
        List<ScanGroupSummaryResponse> result = queryService.findGroups();

        assertEquals(2, result.size());
        assertEquals(newGroup.getId(), result.get(0).id());
        assertEquals(oldGroup.getId(), result.get(1).id());

        ScanGroupSummaryResponse old = result.get(1);
        assertEquals(ScanStatus.PARTIAL, old.status());
        assertEquals(2, old.totalScans());
        assertEquals(1L, old.scansByStatus().get(ScanStatus.COMPLETED));
        assertEquals(1L, old.scansByStatus().get(ScanStatus.FAILED));
        assertEquals(List.of("10.0.0.5", "10.0.0.9"), old.targets());
        assertEquals(2, old.assetsCount());
        assertEquals(2, old.findingsCount());
        assertEquals(240L, old.durationSeconds());
    }

    @Test
    void findGroups_storedCompletedButScanStillRunning_isRunning() {
        ScanGroupSummaryResponse recent = queryService.findGroups().get(0);

        assertEquals(ScanStatus.RUNNING, recent.status());
        assertNull(recent.durationSeconds());
        assertEquals(0, recent.assetsCount());
        assertEquals(0, recent.findingsCount());
    }

    @Test
    void findGroups_withoutGroups_returnsEmpty() {
        groups.clear();
        assertTrue(queryService.findGroups().isEmpty());
    }

    @Test
    void findGroups_groupWithoutScans_keepsTheStoredStatus() {
        ScanExecutionGroup empty = group(T0.plusHours(2));
        empty.setStatus(ScanStatus.PENDING);

        ScanGroupSummaryResponse result = queryService.findGroups().get(0);

        assertEquals(empty.getId(), result.id());
        assertEquals(ScanStatus.PENDING, result.status());
        assertEquals(0, result.totalScans());
    }

    @Test
    void findGroupDetail_listsTheScansWithTheirOwnNumbers() {
        ScanGroupDetailResponse detail = queryService.findGroupDetail(oldGroup.getId());

        assertEquals(ScanStatus.PARTIAL, detail.group().status());
        assertEquals(2, detail.scans().size());

        ScanSummaryResponse ok = detail.scans().get(0);
        assertEquals(okScan.getId(), ok.id());
        assertEquals(oldGroup.getId(), ok.groupId());
        assertEquals("10.0.0.5", ok.target());
        assertEquals(180L, ok.durationSeconds());
        assertEquals(2, ok.assetsCount());
        assertEquals(2, ok.findingsCount());
        assertEquals(1, ok.warningsCount());

        ScanSummaryResponse failed = detail.scans().get(1);
        assertEquals(ScanStatus.FAILED, failed.status());
        assertEquals("Timeout do Nmap", failed.errorMessage());
        assertEquals(0, failed.assetsCount());
        assertEquals(0, failed.warningsCount());
    }

    @Test
    void findGroupDetail_unknownGroup_throwsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> queryService.findGroupDetail(UUID.randomUUID()));
    }

    // ---------------- scan individual ----------------

    @Test
    void findScanDetail_hasCommandWarningsAssetsAndFindings() {
        ScanDetailResponse detail = queryService.findScanDetail(okScan.getId());

        assertEquals(oldGroup.getId(), detail.groupId());
        assertEquals("nmap -sV -O 10.0.0.5", detail.command());
        assertEquals(List.of("OS detection limitada"), detail.warnings());
        assertEquals(180L, detail.durationSeconds());

        assertEquals(2, detail.assets().size());
        var webAsset = detail.assets().stream().filter(a -> "10.0.0.5".equals(a.ipAddress())).findFirst().orElseThrow();
        assertEquals("web-01", webAsset.hostname());
        assertEquals(List.of(22, 80), webAsset.openPorts().stream().map(p -> p.port()).toList()); // só abertas, em ordem

        assertEquals(2, detail.findings().size()); // o SSH_EXPOSED veio de outro scan
        var critical = detail.findings().stream().filter(f -> "CVE-2021-0001".equals(f.code())).findFirst().orElseThrow();
        assertEquals(Severity.CRITICAL, critical.severity());
        assertEquals("10.0.0.5", critical.assetIp());
        assertEquals(80, critical.port());
        assertEquals("tcp", critical.protocol());
    }

    @Test
    void findScanDetail_failedScan_exposesTheError() {
        ScanDetailResponse detail = queryService.findScanDetail(failedScan.getId());

        assertEquals(ScanStatus.FAILED, detail.status());
        assertEquals("Timeout do Nmap", detail.errorMessage());
        assertTrue(detail.assets().isEmpty());
        assertTrue(detail.findings().isEmpty());
    }

    @Test
    void findScanDetail_unknownScan_throwsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> queryService.findScanDetail(UUID.randomUUID()));
    }

    @Test
    void findRecentScans_returnsSummariesWithCounts() {
        List<ScanSummaryResponse> recent = queryService.findRecentScans();

        assertEquals(3, recent.size());
        assertEquals(2, recent.stream().filter(s -> s.id().equals(okScan.getId())).findFirst().orElseThrow().assetsCount());
    }

    // ---------------- construção dos dados ----------------

    private ScanExecutionGroup group(LocalDateTime createdAt) {
        ScanExecutionGroup g = new ScanExecutionGroup();
        g.setCreatedAt(createdAt);
        groups.add(g);
        return g;
    }

    private Scan scan(String target, ScanExecutionGroup group, ScanStatus status, LocalDateTime start, LocalDateTime end) {
        Scan s = new Scan(target, group);
        s.setStatus(status);
        s.setStartedAt(start);
        s.setFinishedAt(end);
        s.setCreatedAt(start != null ? start : T0);
        scans.add(s);
        return s;
    }

    private Asset asset(String ip, String hostname) {
        Asset a = new Asset();
        a.setId(UUID.randomUUID());
        a.setIpAddress(ip);
        a.setHostname(hostname);
        a.setStatus(AssetStatus.UP);
        return a;
    }

    private Service service(Asset asset, int port, String name, boolean open) {
        Service s = new Service(port, "tcp", open ? "open" : "closed", name, "prod", "1.0", null);
        s.setId(UUID.randomUUID());
        s.setAsset(asset);
        services.add(s);
        return s;
    }

    private Finding finding(Asset asset, Service service, String code, Severity severity, Scan scan) {
        Finding f = new Finding();
        f.setId(UUID.randomUUID());
        f.setAsset(asset);
        f.setService(service);
        f.setScan(scan);
        f.setType(code.startsWith("CVE") ? FindingType.VULNERABILITY : FindingType.POLICY_VIOLATION);
        f.setCode(code);
        f.setTitle("Título de " + code);
        f.setSeverity(severity);
        f.setStatus(FindingStatus.OPEN);
        return f;
    }

    // ---------------- repositórios em memória (só o que o service usa) ----------------

    @SuppressWarnings("unchecked")
    private static <R> R fake(Class<R> type, java.util.function.BiFunction<String, Object[], Object> handler) {
        return (R) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            Object[] a = args == null ? new Object[0] : args;
            return switch (method.getName()) {
                case "toString" -> type.getSimpleName() + "Fake";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == a[0];
                default -> handler.apply(method.getName(), a);
            };
        });
    }

    private static List<Object[]> rows(Collection<?> source, Function<Object, Object[]> mapper) {
        return source.stream().map(mapper).toList();
    }

    private static UUID groupIdOf(Scan s) {
        return s.getExecGroup().getId();
    }

    private ScanExecutionGroupRepository groupRepo() {
        return fake(ScanExecutionGroupRepository.class, (name, a) -> switch (name) {
            case "findTop200ByOrderByCreatedAtDesc" -> groups.stream()
                    .sorted((x, y) -> y.getCreatedAt().compareTo(x.getCreatedAt())).toList();
            case "findById" -> groups.stream().filter(g -> g.getId().equals(a[0])).findFirst();
            default -> throw new UnsupportedOperationException(name);
        });
    }

    private ScanRepository scanRepo() {
        return fake(ScanRepository.class, (name, a) -> switch (name) {
            case "findTop200ByOrderByCreatedAtDesc" -> scans.stream()
                    .sorted((x, y) -> y.getCreatedAt().compareTo(x.getCreatedAt())).toList();
            case "findByExecGroupIdOrderByCreatedAtAsc" -> scans.stream()
                    .filter(s -> groupIdOf(s).equals(a[0]))
                    .sorted((x, y) -> x.getCreatedAt().compareTo(y.getCreatedAt())).toList();
            case "findWithExecGroupById" -> scans.stream().filter(s -> s.getId().equals(a[0])).findFirst();
            case "countByGroupAndStatus" -> {
                List<Object[]> result = new ArrayList<>();
                for (Scan s : scans) {
                    if (!((Collection<?>) a[0]).contains(groupIdOf(s))) continue;
                    Object[] row = result.stream().filter(r -> r[0].equals(groupIdOf(s)) && r[1] == s.getStatus()).findFirst().orElse(null);
                    if (row == null) result.add(new Object[] { groupIdOf(s), s.getStatus(), 1L });
                    else row[2] = (Long) row[2] + 1;
                }
                yield result;
            }
            case "findTargetsByGroupIds" -> scans.stream()
                    .filter(s -> ((Collection<?>) a[0]).contains(groupIdOf(s)))
                    .sorted((x, y) -> x.getCreatedAt().compareTo(y.getCreatedAt()))
                    .map(s -> new Object[] { groupIdOf(s), s.getTarget() }).toList();
            case "countWarningsByScanIds" -> scans.stream()
                    .filter(s -> ((Collection<?>) a[0]).contains(s.getId()) && !s.getWarnings().isEmpty())
                    .map(s -> new Object[] { s.getId(), (long) s.getWarnings().size() }).toList();
            default -> throw new UnsupportedOperationException(name);
        });
    }

    private ScanAssetRepository scanAssetRepo() {
        return fake(ScanAssetRepository.class, (name, a) -> switch (name) {
            case "countDistinctAssetsByGroupIds" -> {
                List<Object[]> result = new ArrayList<>();
                for (ScanExecutionGroup g : groups) {
                    if (!((Collection<?>) a[0]).contains(g.getId())) continue;
                    long distinct = scanAssets.stream().filter(sa -> groupIdOf(sa.getScan()).equals(g.getId()))
                            .map(sa -> sa.getAsset().getId()).distinct().count();
                    if (distinct > 0) result.add(new Object[] { g.getId(), distinct });
                }
                yield result;
            }
            case "countByScanIds" -> {
                List<Object[]> result = new ArrayList<>();
                for (Scan s : scans) {
                    long n = scanAssets.stream().filter(sa -> sa.getScan() == s).count();
                    if (n > 0 && ((Collection<?>) a[0]).contains(s.getId())) result.add(new Object[] { s.getId(), n });
                }
                yield result;
            }
            case "findWithAssetByScanId" -> scanAssets.stream().filter(sa -> sa.getScan().getId().equals(a[0])).toList();
            default -> throw new UnsupportedOperationException(name);
        });
    }

    private FindingRepository findingRepo() {
        return fake(FindingRepository.class, (name, a) -> switch (name) {
            case "countByGroupIds" -> {
                List<Object[]> result = new ArrayList<>();
                for (ScanExecutionGroup g : groups) {
                    if (!((Collection<?>) a[0]).contains(g.getId())) continue;
                    long n = findings.stream().filter(f -> f.getScan() != null && groupIdOf(f.getScan()).equals(g.getId())).count();
                    if (n > 0) result.add(new Object[] { g.getId(), n });
                }
                yield result;
            }
            case "countByScanIds" -> {
                List<Object[]> result = new ArrayList<>();
                for (Scan s : scans) {
                    long n = findings.stream().filter(f -> f.getScan() == s).count();
                    if (n > 0 && ((Collection<?>) a[0]).contains(s.getId())) result.add(new Object[] { s.getId(), n });
                }
                yield result;
            }
            case "findWithAssetByScanId" -> findings.stream()
                    .filter(f -> f.getScan() != null && f.getScan().getId().equals(a[0])).toList();
            default -> throw new UnsupportedOperationException(name);
        });
    }

    private ServiceRepository serviceRepo() {
        return fake(ServiceRepository.class, (name, a) -> switch (name) {
            case "findByAssetId" -> services.stream().filter(s -> s.getAsset().getId().equals(a[0])).toList();
            default -> throw new UnsupportedOperationException(name);
        });
    }
}
