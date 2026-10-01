package br.uva.tcc.sentry.Asset.domain;

import java.util.UUID;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * Representa um ativo (Asset) descoberto na rede, como uma máquina, servidor ou dispositivo.
 */
@Getter
@Setter
public class Asset {
    UUID id;
    String ipAddress;
    String hostname;
    String macAddress;
    String operatingSystem;
    AssetStatus status;
    LocalDateTime firstSeenAt;
    LocalDateTime lastSeenAt;
}
