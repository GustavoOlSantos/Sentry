package br.uva.tcc.sentry.scan.domain;

import br.uva.tcc.sentry.Asset.domain.Asset;

import java.util.UUID;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanAsset {
    
    UUID id;
    Scan scan;
    Asset asset;
    LocalDateTime discoveredAt;
    String status;
}
