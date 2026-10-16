package br.uva.tcc.sentry.scan.domain;

import br.uva.tcc.sentry.Asset.domain.Asset;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScanAsset {
    
    Long id;
    Scan scan;
    Asset asset;
    LocalDateTime discoveredAt;
    String status;
}
