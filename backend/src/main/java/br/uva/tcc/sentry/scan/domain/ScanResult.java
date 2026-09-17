package br.uva.tcc.sentry.scan.domain;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ScanResult {

    private final String target;
    private final String command;
    private final String xml;
    private final int exitCode;

    public ScanResult(String target, String command, String xml, int exitCode) {
        this.target = target;
        this.command = command;
        this.xml = xml;
        this.exitCode = exitCode;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }
}