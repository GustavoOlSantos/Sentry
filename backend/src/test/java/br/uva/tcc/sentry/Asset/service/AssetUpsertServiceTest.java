package br.uva.tcc.sentry.Asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.Asset.repository.AssetRepository;
import br.uva.tcc.sentry.Asset.repository.ServiceRepository;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;

class AssetUpsertServiceTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime T2 = T1.plusDays(1);

    private final List<Asset> assets = new ArrayList<>();
    private final List<Service> services = new ArrayList<>();
    private AssetUpsertService upsertService;

    @BeforeEach
    void setUp() {
        upsertService = new AssetUpsertService(assetRepository(), serviceRepository());
    }

    @Test
    void sameIpTwice_reusesTheAsset() {
        DiscoveredHost first = upsertService.upsert(host("10.0.0.5", T1, AssetStatus.UP, "srv-01", service(80, "1.0")));
        DiscoveredHost second = upsertService.upsert(host("10.0.0.5", T2, AssetStatus.UP, null, service(80, "1.0")));

        assertEquals(1, assets.size());
        assertEquals(first.asset().getId(), second.asset().getId());
        assertEquals(T1, second.asset().getFirstSeenAt());
        assertEquals(T2, second.asset().getLastSeenAt());
        assertEquals("srv-01", second.asset().getHostname()); // não apaga com null o que já se sabia
    }

    @Test
    void differentIps_areDifferentAssets() {
        upsertService.upsert(host("10.0.0.5", T1, AssetStatus.UP, null));
        upsertService.upsert(host("10.0.0.6", T1, AssetStatus.UP, null));
        assertEquals(2, assets.size());
    }

    @Test
    void samePortTwice_reusesTheServiceAndUpdatesIt() {
        DiscoveredHost first = upsertService.upsert(host("10.0.0.5", T1, AssetStatus.UP, null, service(80, "1.0")));
        DiscoveredHost second = upsertService.upsert(host("10.0.0.5", T2, AssetStatus.UP, null, service(80, "2.0")));

        assertEquals(1, services.size());
        assertEquals(first.services().get(0).getId(), second.services().get(0).getId());
        assertEquals("2.0", second.services().get(0).getVersion());
        assertEquals(second.asset().getId(), second.services().get(0).getAsset().getId());
    }

    @Test
    void portThatStoppedResponding_isKeptButClosed_andNotReturned() {
        upsertService.upsert(host("10.0.0.5", T1, AssetStatus.UP, null, service(22, "8.0"), service(80, "1.0")));

        DiscoveredHost second = upsertService.upsert(host("10.0.0.5", T2, AssetStatus.UP, null, service(80, "1.0")));

        assertEquals(2, services.size()); // a linha do 22 continua (findings antigos apontam para ela)
        assertEquals(1, second.services().size());
        assertEquals(80, second.services().get(0).getPort());
        assertFalse(find(22).isOpen());
        assertEquals("closed", find(22).getState());
    }

    @Test
    void portThatComesBack_isReopenedWithTheSameRow() {
        DiscoveredHost first = upsertService.upsert(host("10.0.0.5", T1, AssetStatus.UP, null, service(22, "8.0")));
        upsertService.upsert(host("10.0.0.5", T2, AssetStatus.UP, null));

        DiscoveredHost third = upsertService.upsert(host("10.0.0.5", T2.plusDays(1), AssetStatus.UP, null, service(22, "9.0")));

        assertEquals(1, services.size());
        assertEquals(first.services().get(0).getId(), third.services().get(0).getId());
        assertTrue(find(22).isOpen());
        assertEquals("9.0", find(22).getVersion());
    }

    @Test
    void samePortTwiceInTheSameScan_isIgnored() {
        DiscoveredHost result = upsertService.upsert(host("10.0.0.5", T1, AssetStatus.UP, null, service(80, "1.0"), service(80, "1.0")));
        assertEquals(1, result.services().size());
        assertEquals(1, services.size());
    }

    @Test
    void assetStatusIsUpdated() {
        upsertService.upsert(host("10.0.0.5", T1, AssetStatus.UP, null));
        DiscoveredHost second = upsertService.upsert(host("10.0.0.5", T2, AssetStatus.DOWN, null));
        assertEquals(AssetStatus.DOWN, second.asset().getStatus());
        assertNull(second.asset().getOperatingSystem());
    }

    // ---------------- helpers ----------------

    private DiscoveredHost host(String ip, LocalDateTime seenAt, AssetStatus status, String hostname, Service... svcs) {
        Asset asset = new Asset();
        asset.setIpAddress(ip);
        asset.setStatus(status);
        asset.setHostname(hostname);
        asset.setFirstSeenAt(seenAt); // o parser sempre preenche os dois com "agora"
        asset.setLastSeenAt(seenAt);
        return new DiscoveredHost(asset, new ArrayList<>(List.of(svcs)));
    }

    private Service service(int port, String version) {
        return new Service(port, "tcp", "open", "svc", "prod", version, "cpe:2.3:a:x:y:" + version + ":*:*:*:*:*:*:*");
    }

    private Service find(int port) {
        return services.stream().filter(s -> s.getPort() == port).findFirst().orElseThrow();
    }

    private AssetRepository assetRepository() {
        return (AssetRepository) Proxy.newProxyInstance(
                AssetRepository.class.getClassLoader(),
                new Class<?>[] { AssetRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByIpAddress" -> assets.stream().filter(a -> a.getIpAddress().equals(args[0])).findFirst();
                    case "save", "saveAndFlush" -> {
                        Asset a = (Asset) args[0];
                        if (a.getId() == null) {
                            a.setId(UUID.randomUUID());
                            assets.add(a);
                        }
                        yield a;
                    }
                    case "toString" -> "InMemoryAssetRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ServiceRepository serviceRepository() {
        return (ServiceRepository) Proxy.newProxyInstance(
                ServiceRepository.class.getClassLoader(),
                new Class<?>[] { ServiceRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByAssetId" -> services.stream().filter(s -> s.getAsset().getId().equals(args[0])).toList();
                    case "findByAssetIdAndPortAndProtocol" -> services.stream()
                            .filter(s -> s.getAsset().getId().equals(args[0]) && s.getPort().equals(args[1]) && s.getProtocol().equals(args[2]))
                            .findFirst();
                    case "save", "saveAndFlush" -> {
                        Service s = (Service) args[0];
                        if (s.getId() == null) {
                            s.setId(UUID.randomUUID());
                            services.add(s);
                        }
                        yield s;
                    }
                    case "toString" -> "InMemoryServiceRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
