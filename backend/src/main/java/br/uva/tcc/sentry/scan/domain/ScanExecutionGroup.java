package br.uva.tcc.sentry.scan.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scan_execution_group")
@Getter 
@Setter 
public class ScanExecutionGroup {

    // Id gerado na aplicação (construtor), sem @GeneratedValue.
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** 1 grupo : N scans (o FK fica em Scan.exec_group_id). */
    @OneToMany(mappedBy = "execGroup", cascade = CascadeType.ALL)
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

    public void markStarted(){
        this.status = ScanStatus.RUNNING;
        this.setStartedAt(LocalDateTime.now());
    }

    public void markFinished(){
        this.status = ScanStatus.COMPLETED;
        this.setFinishedAt(LocalDateTime.now());
    }

    public void markFailed(){
        this.status = ScanStatus.FAILED;
        this.setFinishedAt(LocalDateTime.now());
    }

    public void markCancelled(){
        this.status = ScanStatus.CANCELLED;
        this.setFinishedAt(LocalDateTime.now());
    }
}
