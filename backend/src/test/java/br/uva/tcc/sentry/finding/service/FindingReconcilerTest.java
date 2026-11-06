package br.uva.tcc.sentry.finding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.finding.repository.FindingRepository;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanCoverage;
import br.uva.tcc.sentry.shared.domain.Severity;

class FindingReconcilerTest {

    private static final LocalDateTime DAY0 = LocalDateTime.of(2026, 11, 1, 12, 0);

    private final List<Finding> store = new ArrayList<>();
    private FindingReconciler reconciler;

    private Asset asset;
    private Service http;
    private Service ssh;

    @BeforeEach
    void setUp() {
        reconciler = new FindingReconciler(inMemoryRepository());

        asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setIpAddress("10.0.0.5");
        asset.setStatus(AssetStatus.UP);

        http = service(80);
        ssh = service(22);
    }

    // ---------------- criação e deduplicação ----------------

    @Test
    void firstScan_createsFindings() {
        var result = scan(1, coverageWithAssetScanned(),
                vuln(http, "CVE-2021-0001"), config(ssh, "SSH_EXPOSED"));

        assertEquals(2, result.created());
        assertEquals(2, store.size());
        store.forEach(f -> {
            assertEquals(FindingStatus.OPEN, f.getStatus());
            assertNotNull(f.getFingerprint());
            assertNotNull(f.getLastSeenAt());
        });
    }

    @Test
    void sameScanTwice_doesNotDuplicate() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"), config(ssh, "SSH_EXPOSED"));
        var second = scan(2, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"), config(ssh, "SSH_EXPOSED"));

        assertEquals(2, store.size());
        assertEquals(0, second.created());
        assertEquals(2, second.refreshed());
        store.forEach(f -> assertEquals(day(2).plusMinutes(5), f.getLastSeenAt()));
    }

    @Test
    void duplicateCandidatesInTheSameScan_areMerged() {
        var result = scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"), vuln(http, "CVE-2021-0001"));

        assertEquals(1, result.created());
        assertEquals(1, store.size());
    }

    @Test
    void sameCveOnDifferentPorts_areDifferentFindings() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"), vuln(service(8080), "CVE-2021-0001"));
        assertEquals(2, store.size());
    }

    // ---------------- ausência + cobertura ----------------

    @Test
    void absentAndCovered_isValidated() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));

        var second = scan(2, coverageWithAssetScanned());

        assertEquals(1, second.validated());
        Finding f = store.get(0);
        assertEquals(FindingStatus.VALIDATED, f.getStatus());
        assertEquals(day(2).plusMinutes(5), f.getValidatedAt());
    }

    @Test
    void absentButAssetWasNotScanned_isUntouched() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));

        scan(2, new ScanCoverage()); // host fora do ar / fora do alvo do scan

        assertEquals(FindingStatus.OPEN, store.get(0).getStatus());
    }

    @Test
    void absentButHostIsDown_isUntouched() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));

        Asset down = new Asset();
        down.setId(asset.getId());
        down.setStatus(AssetStatus.DOWN);
        ScanCoverage coverage = new ScanCoverage();
        coverage.markHostScanned(new DiscoveredHost(down, List.of()));

        scan(2, coverage);

        assertEquals(FindingStatus.OPEN, store.get(0).getStatus());
    }

    @Test
    void absentButServiceCouldNotBeVerified_isUntouched() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));

        ScanCoverage coverage = coverageWithAssetScanned();
        coverage.markServiceUnverified(asset, http); // NVD fora do ar ou versão não detectada
        scan(2, coverage);

        assertEquals(FindingStatus.OPEN, store.get(0).getStatus());
    }

    @Test
    void unverifiedService_doesNotBlockPolicyFindingsOfTheSamePort() {
        scan(1, coverageWithAssetScanned(), config(http, "EXPOSED_DATABASE_PORT"));

        ScanCoverage coverage = coverageWithAssetScanned();
        coverage.markServiceUnverified(asset, http);
        scan(2, coverage);

        assertEquals(FindingStatus.VALIDATED, store.get(0).getStatus());
    }

    @Test
    void versionUpgrade_validatesOldCveAndOpensTheNewOne() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));

        var second = scan(2, coverageWithAssetScanned(), vuln(http, "CVE-2023-9999"));

        assertEquals(1, second.created());
        assertEquals(1, second.validated());
        assertEquals(FindingStatus.VALIDATED, byCode("CVE-2021-0001").getStatus());
        assertEquals(FindingStatus.OPEN, byCode("CVE-2023-9999").getStatus());
    }

    @Test
    void justified_isNeverClosedByAbsence() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));
        store.get(0).changeStatusByUser(FindingStatus.JUSTIFIED, "risco aceito", day(1).plusHours(6));

        scan(2, coverageWithAssetScanned());

        assertEquals(FindingStatus.JUSTIFIED, store.get(0).getStatus());
    }

    // ---------------- reteste ----------------

    @Test
    void resolvedAndGone_isValidated() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));
        store.get(0).changeStatusByUser(FindingStatus.RESOLVED, null, day(1).plusHours(6));

        scan(2, coverageWithAssetScanned());

        assertEquals(FindingStatus.VALIDATED, store.get(0).getStatus());
    }

    @Test
    void resolvedButStillThere_failsValidation() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));
        store.get(0).changeStatusByUser(FindingStatus.RESOLVED, null, day(1).plusHours(6));

        var second = scan(2, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));

        assertEquals(1, second.validationFailed());
        assertEquals(FindingStatus.FAILED_VALIDATION, store.get(0).getStatus());
    }

    @Test
    void failedValidationThenFixed_isValidatedOnTheNextScan() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));
        store.get(0).changeStatusByUser(FindingStatus.RESOLVED, null, day(1).plusHours(6));
        scan(2, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));
        store.get(0).changeStatusByUser(FindingStatus.RESOLVED, null, day(2).plusHours(6));

        scan(3, coverageWithAssetScanned());

        assertEquals(FindingStatus.VALIDATED, store.get(0).getStatus());
    }

    // ---------------- regressão ----------------

    @Test
    void validatedThatComesBack_isReopened() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));
        scan(2, coverageWithAssetScanned());
        assertEquals(FindingStatus.VALIDATED, store.get(0).getStatus());

        var third = scan(3, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));

        assertEquals(1, third.reopened());
        assertEquals(1, store.size());
        assertEquals(FindingStatus.OPEN, store.get(0).getStatus());
        assertEquals(1, store.get(0).getReopenCount());
        assertNull(store.get(0).getValidatedAt());
    }

    // ---------------- ScanCoverage ----------------

    @Test
    void coverage_vulnerabilityOfAServiceThatNoLongerExists_isCovered() {
        scan(1, coverageWithAssetScanned(), vuln(http, "CVE-2021-0001"));

        assertTrue(coverageWithAssetScanned().covers(store.get(0)));
        assertFalse(new ScanCoverage().covers(store.get(0)));
    }

    // ---------------- helpers ----------------

    private LocalDateTime day(int n) {
        return DAY0.plusDays(n);
    }

    /** Executa um scan (que começa em day(n) e é reconciliado 5 minutos depois). */
    private FindingReconciler.Result scan(int n, ScanCoverage coverage, Finding... candidates) {
        Scan scan = new Scan(UUID.randomUUID(), asset.getIpAddress());
        scan.setStartedAt(day(n));
        return reconciler.reconcile(scan, new ArrayList<>(List.of(candidates)), coverage, day(n).plusMinutes(5));
    }

    private ScanCoverage coverageWithAssetScanned() {
        ScanCoverage coverage = new ScanCoverage();
        coverage.markHostScanned(new DiscoveredHost(asset, List.of(http, ssh)));
        return coverage;
    }

    private Service service(int port) {
        Service s = new Service(port, "tcp", "open", "svc", "prod", "1.0", "cpe:2.3:a:x:y:1.0:*:*:*:*:*:*:*");
        s.setId(UUID.randomUUID());
        s.setAsset(asset);
        return s;
    }

    private Finding vuln(Service service, String cve) {
        return candidate(service, FindingType.VULNERABILITY, cve);
    }

    private Finding config(Service service, String code) {
        return candidate(service, FindingType.POLICY_VIOLATION, code);
    }

    private Finding candidate(Service service, FindingType type, String code) {
        Finding f = new Finding();
        f.setAsset(asset);
        f.setService(service);
        f.setType(type);
        f.setCode(code);
        f.setTitle(code);
        f.setSeverity(Severity.HIGH);
        f.setStatus(FindingStatus.OPEN);
        f.setEvidence("evidência de " + code);
        f.setDetectedAt(LocalDateTime.now());
        return f;
    }

    private Finding byCode(String code) {
        return store.stream().filter(f -> code.equals(f.getCode())).findFirst().orElseThrow();
    }

    /** Repositório em memória: só implementa os métodos que o reconciler usa. */
    private FindingRepository inMemoryRepository() {
        return (FindingRepository) Proxy.newProxyInstance(
                FindingRepository.class.getClassLoader(),
                new Class<?>[] { FindingRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "saveAll" -> {
                        for (Object o : (Iterable<?>) args[0]) {
                            store.add((Finding) o);
                        }
                        yield args[0];
                    }
                    case "findByFingerprintIn" -> store.stream()
                            .filter(f -> ((Collection<?>) args[0]).contains(f.getFingerprint()))
                            .toList();
                    case "findTrackedByAssetIds" -> store.stream()
                            .filter(f -> ((Collection<?>) args[0]).contains(f.getAsset().getId()))
                            .filter(f -> ((Collection<?>) args[1]).contains(f.getStatus()))
                            .toList();
                    case "toString" -> "InMemoryFindingRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
