package br.uva.tcc.sentry.scan.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ScanExecutionGroup {
    private UUID id;
    private ScanStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<Scan> scans;

    public ScanExecutionGroup(){
        this.id = UUID.randomUUID();
        this.status = ScanStatus.PENDING;
        this.setCreatedAt(LocalDateTime.now());
        scans = new ArrayList<Scan>();
    }

    public void addScan(Scan scan){
        this.scans.add(scan);
    }
}
