package br.uva.tcc.sentry.scan.scanner;

import br.uva.tcc.sentry.scan.domain.ScanResult;

public interface ScanEngine {
    ScanResult scan(String target);
}
