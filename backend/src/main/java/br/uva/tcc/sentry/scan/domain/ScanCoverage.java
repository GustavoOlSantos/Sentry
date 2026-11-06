package br.uva.tcc.sentry.scan.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingType;

/**
 * O que um scan realmente "conseguiu enxergar". Existe porque NÃO ver um
 * finding só significa "foi corrigido" se o scan tinha condição de vê-lo:
 * host fora do ar, consulta ao NVD que falhou ou versão não detectada não
 * provam nada, e tratar isso como correção fecharia findings que seguem
 * abertos.
 *
 * Regras de cobertura de um finding:
 * <ul>
 *   <li>o asset precisa ter sido encontrado (e estar no ar) neste scan;</li>
 *   <li>findings de vulnerabilidade de um serviço só são cobertos se o serviço
 *       foi verificado contra o NVD (tinha CPE e versão, e a consulta deu
 *       certo). Se a porta fechou e o serviço nem aparece mais, está coberto:
 *       o problema realmente sumiu.</li>
 * </ul>
 *
 * Limitação conhecida: checagens de configuração que fazem sua própria
 * sondagem de rede (ex.: handshake TLS do {@code ExpiredCertificateCheck})
 * engolem falhas e devolvem "nada encontrado"; por ora isso é tratado como
 * "sem problema". Para tornar essas checagens conclusivas, elas precisariam
 * sinalizar "não consegui verificar" (ver {@link #markServiceUnverified}).
 */
public class ScanCoverage {

    private final Set<UUID> scannedAssetIds = new HashSet<>();
    private final Set<String> unverifiedServiceKeys = new HashSet<>();

    /** Registra um host que respondeu ao scan. Host fora do ar não conta como varrido. */
    public void markHostScanned(DiscoveredHost host) {
        Asset asset = host.asset();
        if (asset.getId() != null && asset.getStatus() != AssetStatus.DOWN) {
            scannedAssetIds.add(asset.getId());
        }
    }

    /** O serviço foi visto, mas não deu para checar suas vulnerabilidades (sem CPE/versão ou NVD indisponível). */
    public void markServiceUnverified(Asset asset, Service service) {
        unverifiedServiceKeys.add(serviceKey(asset.getId(), service.getPort(), service.getProtocol()));
    }

    public Set<UUID> scannedAssetIds() {
        return Collections.unmodifiableSet(scannedAssetIds);
    }

    /** Este scan teria condição de ver o finding caso o problema ainda existisse? */
    public boolean covers(Finding finding) {
        UUID assetId = finding.getAsset().getId();

        if (!scannedAssetIds.contains(assetId)) {
            return false;
        }

        Service service = finding.getService();
        if (finding.getType() == FindingType.VULNERABILITY && service != null) {
            return !unverifiedServiceKeys.contains(serviceKey(assetId, service.getPort(), service.getProtocol()));
        }

        return true;
    }

    public static String serviceKey(UUID assetId, Integer port, String protocol) {
        return assetId + "|" + port + "/" + protocol;
    }
}
