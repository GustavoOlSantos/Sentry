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
    private String command;
    private ScanStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public Scan(String target){
        this.id = UUID.randomUUID();
        this.status = ScanStatus.PENDING;
        this.createdAt = LocalDateTime.now();

        this.target = target;
    }

    public Scan() {
    }

    public void ScanStarted(){
        this.startedAt = LocalDateTime.now();
        this.status = ScanStatus.RUNNING;
    }

    public void markFinished() {
        this.finishedAt = LocalDateTime.now();
        this.status = ScanStatus.COMPLETED;
    }
}
