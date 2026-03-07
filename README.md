# MiddleTier-Backup

A Spring Boot REST API service for managing file backups, directory synchronization, and media file imports. It tracks the file system in a database, runs scheduled backups, and provides a REST API for configuration and operational control.

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4.10 (Jetty) |
| Database | MySQL (production), H2 (test) |
| Migrations | Liquibase |
| Build | Maven |
| Coverage | JaCoCo |
| Quality | SonarCloud |

## Features

- **Scheduled backups** — runs `mysqldump` on a configurable cron schedule; organises output into dated directories
- **File synchronization** — mirrors source directories to destination directories and tracks differences in the database
- **Media import pipeline** — three-stage pipeline (pre-import → import → post-import) for processing and organising incoming files, with configurable thread count
- **File system tracking** — indexes files and directories in the database, stores metadata (EXIF, image size, GPS coordinates)
- **Classifications** — rules-based classification of files by extension/type
- **Labels** — user-defined tags that can be applied to files
- **Print selection** — workflow for selecting and tracking files chosen for printing
- **Hardware registry** — tracks known hardware devices by MAC address
- **Action queue** — pending file actions (copy, delete, etc.) with email notification support
- **Health & metrics** — Spring Boot Actuator endpoints exposed for monitoring

## Building

```bash
# Full build including integration tests
mvn verify

# Skip integration tests
mvn package -Dskip.surefire.tests=true
```

The build produces an executable JAR and a deployment zip assembly.

## Running

The service uses Spring profiles to select the configuration:

| Profile | Purpose |
|---|---|
| *(default)* | Local/debug using H2 in-memory database |
| `dev` | Development environment |
| `pdn` | Production (MySQL, port 12013) |

```bash
# Run locally (H2, debug)
java -jar target/MiddleTier-Backup-*.jar

# Run with a specific profile
java -jar target/MiddleTier-Backup-*.jar --spring.profiles.active=pdn
```

In production the service runs as a systemd unit (`middletier-backup.service`) on port **12013**.

## Configuration

All custom properties are under the `backup.*` namespace. Key properties:

| Property | Description | Default |
|---|---|---|
| `backup.enabled` | Enable/disable scheduled backups | `false` |
| `backup.schedule` | Cron expression for the backup schedule | `0 20 0/1 * * ?` |
| `backup.gather-enabled` | Enable file system gather on schedule | `false` |
| `backup.gather-schedule` | Cron expression for the gather | `0 0 0 * * ?` |
| `backup.directory.name` | Root directory for backup output | — |
| `backup.directory.date-format` | Date format used for daily subdirectories | `yyyy-MM-dd` |
| `backup.directory.days` | Number of daily backup directories to retain | — |
| `backup.import-threads` | Number of parallel threads for file import | `5` |
| `backup.db-backup-command` | Shell command template for `mysqldump` | see `application.yml` |
| `backup.ffmpeg-command` | Shell command for re-encoding video files | see `application.yml` |
| `backup.vid-to-image-command` | Shell command to extract a thumbnail from video | see `application.yml` |
| `backup.email.host` | SMTP host for action notification emails | — |
| `backup.email.port` | SMTP port | — |
| `backup.email.to` | Recipient address | — |
| `backup.email.from` | Sender address | — |
| `backup.email.enabled` | Enable email sending | `false` |

Production database credentials are supplied via environment variables:
- `db.pdn.backup.server`
- `db.pdn.backup.user`
- `db.pdn.backup.password`

## REST API

The API is self-documented via Swagger UI at `/swagger-ui.html` when the service is running.

### Base paths

| Base path | Purpose |
|---|---|
| `/jbr/ext/backup` | Configuration — locations, sources, synchronizations, classifications |
| `/jbr/int/backup` | Operations — actions, labels, prints, summary |
| `/jbr/ext/hardware` | Hardware registry |

### Key endpoints

#### Configuration (`/jbr/ext/backup`)

| Method | Path | Description |
|---|---|---|
| GET/POST/PUT/DELETE | `/location` | Manage physical locations |
| GET/POST/PUT/DELETE | `/source` | Manage backup/sync source directories |
| GET/POST/PUT/DELETE | `/importSource` | Manage import source directories |
| GET/POST/PUT/DELETE | `/preImportSource` | Manage pre-import staging directories |
| GET/POST/PUT/DELETE | `/postImportSource` | Manage post-import output directories |
| GET/POST/PUT/DELETE | `/synchronize` | Manage synchronization pairs |
| GET/POST/PUT/DELETE | `/classification` | Manage file classifications |

#### Operations (`/jbr/int/backup`)

| Method | Path | Description |
|---|---|---|
| GET | `/actions` | List pending actions |
| POST | `/actions` | Confirm an action |
| GET | `/confirmed-actions` | List confirmed actions |
| POST | `/actionemail` | Send action summary email |
| GET | `/summary` | Get current backup summary |
| GET | `/ignore` | List ignored files |
| GET/POST/DELETE | `/label`, `/labels` | Manage file labels |
| GET/POST/PUT/DELETE | `/print`, `/prints` | Manage print selections |

#### Hardware (`/jbr/ext/hardware`)

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all hardware |
| GET | `/byId?macAddress=` | Get hardware by MAC address |
| POST/PUT/DELETE | `/` | Create, update, or delete a hardware entry |

### Actuator

Spring Boot Actuator is fully exposed. The health endpoint includes service-specific details:

```
GET /actuator/health
```

## Database Migrations

Schema is managed by Liquibase. Changelogs are in `src/main/resources/db/changelog/`. A separate set of H2-compatible changelogs exists under `db/changelog/h2/` for local and test use.

## Project Structure

```
src/main/java/com/jbr/middletier/backup/
  config/       Application configuration and properties
  control/      REST controllers
  data/         JPA entities
  dataaccess/   Spring Data repositories
  dto/          Data transfer objects
  exception/    Custom exceptions and REST error handler
  filetree/     File tree abstraction (real-world and database representations)
  manager/      Business logic managers
  manager/importing/  Multi-step import pipeline
  schedule/     Scheduled tasks (backup, gather)
  util/         Utilities (geo-coordinates, image metadata, file search)
```