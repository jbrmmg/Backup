package com.jbr.middletier.backup.data;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "backup_job_run")
public class BackupJobRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "backup_id")
    private String backupId;

    @Column(name = "run_date")
    private LocalDate runDate;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "status")
    private String status;

    @Column(name = "message", length = 500)
    private String message;

    protected BackupJobRun() {}

    public BackupJobRun(String backupId) {
        this.backupId = backupId;
        this.runDate = LocalDate.now();
        this.startedAt = LocalDateTime.now();
        this.status = RunStatus.RUNNING.name();
    }

    public void reset() {
        this.status = RunStatus.RUNNING.name();
        this.startedAt = LocalDateTime.now();
        this.finishedAt = null;
        this.message = null;
    }

    public void complete(RunStatus runStatus, String completionMessage) {
        this.status = runStatus.name();
        this.finishedAt = LocalDateTime.now();
        this.message = completionMessage;
    }
}
