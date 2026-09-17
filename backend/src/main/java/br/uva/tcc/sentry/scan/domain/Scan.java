package br.uva.tcc.sentry.scan.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class Scan {
    private UUID id;
    private String target;
    private ScanStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String command;
    private String errorMessage;
    private LocalDateTime createdAt;
}
