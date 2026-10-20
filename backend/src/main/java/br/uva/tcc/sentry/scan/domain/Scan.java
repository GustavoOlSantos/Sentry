package br.uva.tcc.sentry.scan.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scan")
@Getter 
@Setter 
public class Scan {

    // O id é gerado na própria aplicação (UUID.randomUUID() nos construtores), pois é
    // publicado na fila antes de o scan ser persistido. Por isso não há @GeneratedValue.
    @Id
    private UUID id;

    /** N scans : 1 grupo de execução. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exec_group_id")
    private ScanExecutionGroup execGroup;

    @Column(nullable = false)
    private String target;

    @Column(length = 2000)
    private String command;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanStatus status;

    @ElementCollection
    @CollectionTable(name = "scan_warning", joinColumns = @JoinColumn(name = "scan_id"))
    @Column(name = "warning", length = 1000)
    private List<String> warnings = new ArrayList<>();

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
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
