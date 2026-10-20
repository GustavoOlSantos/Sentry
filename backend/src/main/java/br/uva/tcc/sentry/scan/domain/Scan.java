package br.uva.tcc.sentry.scan.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class Scan {
    
    private UUID id;
    private ScanExecutionGroup execGroup;
    private String target;
    private String command;
    private ScanStatus status;
    private List<String> warnings = new ArrayList<>();
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

    public Scan(String target, ScanExecutionGroup execGroup){
        this.id = UUID.randomUUID();
        this.status = ScanStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.execGroup = execGroup;

        this.target = target;
    }

    public Scan(UUID id, String target){
        this.id = id;
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

    public void markFailed() {
        this.finishedAt = LocalDateTime.now();
        this.status = ScanStatus.FAILED;
    }

    public void markNotFounded() {
        this.finishedAt = LocalDateTime.now();
        this.status = ScanStatus.NOT_FOUND;
    }

    public void markCanceled() {
        this.finishedAt = LocalDateTime.now();
        this.status = ScanStatus.CANCELLED;
    }

    public void addWarning(String warning){
        if (warning != null && !warning.isBlank()) {
            warnings.add(warning);
        }   
    }

}
