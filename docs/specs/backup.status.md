# JBR-602 — Backup Status Reporting & Scheduler Refactor

## Overview

The backup service runs several scheduled backup jobs (file, database, git, zip). This work addresses two related problems: the scheduler that triggers jobs has fragile time-window logic that can cause double-execution, and there is no structured way to query whether a recent run succeeded or failed. The changes simplify the scheduling mechanism and add per-job run status tracking with a REST API to surface it.

---

## Problem Statement

### Scheduler
- `BackupCtrl.scheduleBackup()` fires every hour and selects backups whose `time` value falls within a rolling 2-hour window (`endTime - 120` to `endTime` in HHMM integer arithmetic). This arithmetic is not clock-correct — e.g. `HHMM=180` ("1:80") can appear inside a window — and backups at the boundary of the window can be selected in two consecutive hourly runs, relying solely on each backup type's file-existence idempotency check to avoid running twice.
- The `time` field on `Backup` was intended to schedule jobs at specific times of day, but because all selected backups run sequentially in a single invocation, it does not actually stagger execution — it only groups them.

### Status reporting
- `PerformBackup.performBackup()` returns `void`. Callers cannot distinguish a silent skip (e.g. "already backed up") from a success or a failure.
- `BackupCtrl.performBackups()` swallows exceptions at the outer level with a generic error log. Individual job outcomes are not recorded.
- There is no REST endpoint to poll "what happened the last time backup X ran?"
- `DbLoggingManager` persists raw freeform log lines that are useful for debugging but not for health dashboards or alerting.

---

## Goals

1. Replace the time-window scheduler with a single nightly run that executes all jobs sequentially in a defined order.
2. Record the outcome (started, succeeded, failed, skipped) and timestamps for every backup job run.
3. Expose REST endpoints to query the last-run status for all backup jobs and the history for a specific job.
4. Keep the implementation close to the existing `DbLoggingManager` / `Backup` entity pattern — avoid introducing a separate persistence framework.

---

## Out of Scope

- `GatherSynchronizeCtrl` status tracking — covered by a separate ticket.
- Real-time / streaming status (WebSocket, SSE) — polling the new REST endpoints is sufficient for now.
- Per-file-within-a-run detail (already partially covered by `DbLog` entries with a `backup` reference).
- UI changes — this spec covers the backend API layer only.

---

## Proposed Data Model

### New entity: `BackupJobRun`

Tracks one execution of a single `Backup` job.

| Column         | Type             | Notes                                              |
|----------------|------------------|----------------------------------------------------|
| `id`           | `BIGINT` (PK, auto) |                                                 |
| `backup_id`    | `VARCHAR`        | FK → `backup.id`                                   |
| `started_at`   | `DATETIME`       |                                                    |
| `finished_at`  | `DATETIME`       | `NULL` while running                               |
| `status`       | `VARCHAR(10)`    | `RUNNING`, `SUCCESS`, `SKIPPED`, `FAILED`          |
| `message`      | `VARCHAR(500)`   | Optional detail (e.g. exception message, skip reason) |

This table should be managed by a Liquibase changeset under `src/main/resources/db/changelog/`.

---

## Proposed Status Enum

```java
public enum RunStatus {
    RUNNING,   // job has started but not yet completed
    SUCCESS,   // job completed without error
    SKIPPED,   // job determined it had nothing to do (e.g. already backed up)
    FAILED     // job threw an exception or reported an error
}
```

---

## `PerformBackup` Interface Change

Change the interface to return outcome information and expose a summary, and remove the `DbLoggingManager` dependency:

```java
// Before
void performBackup(BackupManager backupManager,
                   DbLoggingManager loggingManager,
                   FileSystem fileSystem,
                   Backup backup);

// After
RunStatus performBackup(BackupManager backupManager,
                        FileSystem fileSystem,
                        Backup backup);

String getSummary();
```

`getSummary()` is called by `BackupCtrl` after a successful run and returns a short human-readable description of what was done. Each implementation should hold enough state from the most recent `performBackup()` call to produce this string. The required format per type is:

| Type | Summary format | Example |
|------|---------------|---------|
| `CleanBackup` | Comma-separated list of directories deleted | `"Deleted: /backups/2026-07-01, /backups/2026-07-02"` |
| `DatabaseBackup` | `"Database <artifact> backed up"` | `"Database mydb backed up"` |
| `FileBackup` | `"<artifact> backed up"` | `"config.tar.gz backed up"` |
| `GitBackup` | `"<directory> backed up"` | `"/home/jason/Source/MyRepo backed up"` |
| `ZipupBackup` | `"Zip created, <n> files"` | `"Zip created, 42 files"` |

Each implementation (`DatabaseBackup`, `FileBackup`, `GitBackup`, `CleanBackup`, `ZipupBackup`) should:
- Remove all `DbLoggingManager` calls — no log entries are written to the old log table.
- Return `RunStatus.SKIPPED` where it currently returns early (e.g. "already backed up, exiting").
- Return `RunStatus.SUCCESS` on normal completion.
- Catch exceptions internally, store the exception message for the caller to retrieve, and return `RunStatus.FAILED` rather than swallowing or rethrowing.

`BackupCtrl` should remove its own `DbLoggingManager` dependency and no longer pass it to `performBackup()`.

---

## `BackupCtrl` Changes

### Scheduler simplification

Replace the current `scheduleBackup()` implementation:

- Remove the HHMM time-window query (`BackupSpecifications.backupsBetweenTimes`).
- On each scheduled trigger, load **all** backups ordered by `time` ascending. The `time` field is repurposed as a sequence/ordering number (lower value runs first) rather than a clock time.
- The trigger cron expression (`backup.schedule` in `application.yml`) already controls when the nightly run fires — no schema change needed for that.
- `BackupSpecifications` and its time-window query can be removed as they will no longer be used.

### Status tracking

`performBackups()` should:

1. Before calling `performBackup(...)`, insert a `BackupJobRun` row with `status = RUNNING` and `started_at = now()`.
2. After the call, update the row with `finished_at = now()` and `status` from the returned `RunStatus`:
   - On `SUCCESS`: set `message` from `performBackup.getSummary()`.
   - On `FAILED`: set `message` from the exception message held by the implementation.
   - On `SKIPPED`: `message` may be left null or set to a brief reason by the implementation.
3. On exception from the outer `initialiseDay()`, mark all pending jobs `FAILED` with the exception message.

---

## New REST Endpoints

All under the existing `/jbr/ext/backup/` prefix to match the URL pattern convention.

### GET `/jbr/ext/backup/status/jobs`

Returns the latest run record for every configured backup job.

**Response (example):**
```json
[
  {
    "backupId": "DB_MAIN",
    "startedAt": "2026-08-07T03:00:01",
    "finishedAt": "2026-08-07T03:00:14",
    "status": "SUCCESS",
    "message": null
  },
  {
    "backupId": "FILES_HOME",
    "startedAt": "2026-08-07T03:00:14",
    "finishedAt": null,
    "status": "RUNNING",
    "message": null
  }
]
```

### GET `/jbr/ext/backup/status/jobs/{backupId}`

Returns the last N runs (default 10) for a specific backup job, newest first. Allows trend/history visibility.

**Query param:** `?limit=10`

---

## DTO Design

```java
public class BackupJobRunDTO {
    private String backupId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;   // RunStatus name
    private String message;
}
```

---

## Log Retention

`BackupJobRun` rows should be retained for a configurable number of days (default 7), controlled by a new property `backup.job-run-retention-days`. Pruning should run at the start of `BackupCtrl.scheduleBackup()` before any jobs are executed, deleting rows older than the configured threshold directly via `BackupJobRunRepository`.

---

## Open Questions

1. **`time` field rename**: Now that `time` is a sequence number rather than a clock time, it should arguably be renamed to `sequence` or `order`. This is a schema + entity change — worth doing as part of this ticket or deferred?
2. **Concurrent run protection**: Should `BackupCtrl` check for any `RUNNING` rows before starting? With a single nightly cron this is unlikely, but guards against a manually triggered run overlapping a scheduled one.
3. **HTTP status code for RUNNING jobs**: Should `GET /status/jobs` return `202 Accepted` if any job is currently `RUNNING`, or always `200`? Recommendation: always `200` and let the client interpret `status`.
4. **Alerting hook**: Is there a desire to push a notification (e.g. email via `ActionManager`) when a job transitions to `FAILED`? Out of scope for this ticket but worth flagging.

---

## Acceptance Criteria

- [ ] `BackupCtrl.scheduleBackup()` loads all backups ordered by `time` and no longer uses a time-window query.
- [ ] `BackupSpecifications` is removed (no longer referenced).
- [ ] `backup_job_run` table is created via Liquibase.
- [ ] Every `PerformBackup` implementation returns a `RunStatus`.
- [ ] `BackupCtrl` writes a `BackupJobRun` row for each job execution (start + end).
- [ ] `GET /jbr/ext/backup/status/jobs` returns the latest run per job.
- [ ] `GET /jbr/ext/backup/status/jobs/{id}` returns history for a specific job.
- [ ] Old run rows older than the configured retention period are pruned.
- [ ] No `PerformBackup` implementation references `DbLoggingManager`.
- [ ] `BackupCtrl` does not reference `DbLoggingManager`.
- [ ] `BackupJobRun.message` is populated with the summary on success and the exception message on failure.
- [ ] Unit tests cover `RunStatus` return values and `getSummary()` output from each backup type.
- [ ] Integration tests cover the new REST endpoints.

---

## Affected Files (expected)

| File | Change |
|------|--------|
| `type/PerformBackup.java` | Return type `void` → `RunStatus`; add `getSummary()`; remove `DbLoggingManager` param |
| `type/DatabaseBackup.java` | Return `RunStatus`; implement `getSummary()`; remove `DbLoggingManager` usage |
| `type/FileBackup.java` | Return `RunStatus`; implement `getSummary()`; remove `DbLoggingManager` usage |
| `type/GitBackup.java` | Return `RunStatus`; implement `getSummary()`; remove `DbLoggingManager` usage |
| `type/CleanBackup.java` | Return `RunStatus`; implement `getSummary()`; remove `DbLoggingManager` usage |
| `type/ZipupBackup.java` | Return `RunStatus`; implement `getSummary()`; remove `DbLoggingManager` usage |
| `schedule/BackupCtrl.java` | Replace time-window query with full ordered load; write `BackupJobRun` rows; remove `DbLoggingManager` dependency |
| `dataaccess/BackupSpecifications.java` | Delete (no longer used) |
| `data/BackupJobRun.java` | New entity |
| `data/RunStatus.java` | New enum |
| `dataaccess/BackupJobRunRepository.java` | New repository |
| `dto/BackupJobRunDTO.java` | New DTO |
| `control/BackupStatusController.java` | New controller |
| `dataaccess/BackupJobRunRepository.java` | Add a delete-by-date query for pruning |
| `src/main/resources/db/changelog/` | New Liquibase changeset |
