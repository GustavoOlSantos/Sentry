package br.uva.tcc.sentry.Asset.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.Asset.repository.AssetRepository;
import br.uva.tcc.sentry.Asset.repository.ServiceRepository;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import lombok.RequiredArgsConstructor;

/**
 * Dá identidade estável aos assets e serviços descobertos por um scan.
 *
 * O parser do Nmap sempre devolve objetos novos (sem id). Antes de persistir,
 * este componente procura o que já existe — asset pelo IP e serviço por
 * porta/protocolo dentro do asset — e atualiza em vez de duplicar. Sem isso
 * os ids mudariam a cada scan e nenhum finding poderia ser reconhecido depois.
 *
 * Propositalmente SEM {@code @Transactional}: cada chamada ao repositório
 * roda na sua transação, assim uma violação de unicidade causada por dois
 * scans simultâneos do mesmo IP pode ser capturada e resolvida aqui (mesma
 * estratégia de {@code obterOuSalvarVulnerability} no ScanConsumer).
 */
@Component
@RequiredArgsConstructor
public class AssetUpsertService {

    private static final Logger log = LoggerFactory.getLogger(AssetUpsertService.class);

    private final AssetRepository assetRepository;
    private final ServiceRepository serviceRepository;

    /**
     * @return o host com o {@link Asset} e os {@link Service}s já persistidos —
     *         apenas os serviços abertos neste scan, que é o que os checkers
     *         e a consulta ao NVD precisam enxergar.
     */
    public DiscoveredHost upsert(DiscoveredHost discovered) {
        Asset asset = upsertAsset(discovered.asset());
        List<Service> openServices = upsertServices(asset, discovered.services());
        return new DiscoveredHost(asset, openServices);
    }

    private Asset upsertAsset(Asset observed) {
        LocalDateTime seenAt = observed.getLastSeenAt() != null ? observed.getLastSeenAt() : LocalDateTime.now();

        Optional<Asset> known = assetRepository.findByIpAddress(observed.getIpAddress());
        if (known.isPresent()) {
            return refresh(known.get(), observed, seenAt);
        }

        if (observed.getFirstSeenAt() == null) {
            observed.setFirstSeenAt(seenAt);
        }
        observed.setLastSeenAt(seenAt);

        try {
            return assetRepository.saveAndFlush(observed);
        } catch (DataIntegrityViolationException e) {
            log.debug("Asset {} criado por outro scan ao mesmo tempo; reaproveitando.", observed.getIpAddress());
            Asset concurrent = assetRepository.findByIpAddress(observed.getIpAddress()).orElseThrow(() -> e);
            return refresh(concurrent, observed, seenAt);
        }
    }

    private Asset refresh(Asset known, Asset observed, LocalDateTime seenAt) {
        known.refreshFrom(observed, seenAt);
        return assetRepository.save(known);
    }

    private List<Service> upsertServices(Asset asset, List<Service> observedServices) {
        Map<String, Service> known = new HashMap<>();
        for (Service service : serviceRepository.findByAssetId(asset.getId())) {
            known.putIfAbsent(service.key(), service);
        }

        Set<String> observedKeys = new HashSet<>();
        List<Service> open = new ArrayList<>();

        for (Service observed : observedServices) {
            if (!observedKeys.add(observed.key())) {
                continue; // mesma porta/protocolo duas vezes no mesmo scan
            }

            Service existing = known.get(observed.key());
            open.add(existing != null ? refresh(existing, observed) : create(asset, observed));
        }

        // portas que o asset tinha abertas e agora não respondem mais
        for (Service stale : known.values()) {
            if (!observedKeys.contains(stale.key()) && stale.isOpen()) {
                stale.markClosed();
                serviceRepository.save(stale);
            }
        }

        return open;
    }

    private Service refresh(Service known, Service observed) {
        known.updateFrom(observed);
        return serviceRepository.save(known);
    }

    private Service create(Asset asset, Service observed) {
        observed.setAsset(asset);

        try {
            return serviceRepository.saveAndFlush(observed);
        } catch (DataIntegrityViolationException e) {
            Service concurrent = serviceRepository
                    .findByAssetIdAndPortAndProtocol(asset.getId(), observed.getPort(), observed.getProtocol())
                    .orElseThrow(() -> e);
            return refresh(concurrent, observed);
        }
    }
}
