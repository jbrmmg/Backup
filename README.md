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
| CI/CD | GitHub Actions (self-hosted runners) |
| Containers | Docker + Docker Compose |
| Integration tests | TestContainers (MySQL) |

## Features

- **Scheduled backups** — runs `mysqldump` on a configurable cron schedule; organises output into dated directories; tracks per-job run history (start time, finish time, status, message)
- **File synchronization** — mirrors source directories to destination directories and tracks differences in the database; records sync start/end time and per-run counts on the sync pair
- **Media import pipeline** — three-stage pipeline (pre-import → import → post-import) for processing and organising incoming files, with configurable thread count
- **File system tracking** — indexes files and directories in the database, stores metadata (EXIF, image size, GPS coordinates)
- **Classifications** — rules-based classification of files by extension/type; each classification carries flags for `image`, `video`, and `browser` (whether the file can be viewed directly in a browser)
- **File search** — full-criteria search across backed-up files by filename glob, date range, size range, label, GPS bounding-box, and expiry range; results are paged
- **Labels** — user-defined tags that can be applied to files
- **Print selection** — workflow for selecting and tracking files chosen for printing, with per-file size selection
- **Hardware registry** — tracks known hardware devices by MAC address
- **Action queue** — pending file actions (copy, delete, etc.) with email notification support
- **Scheduled summary refresh** — summary data is rebuilt on a configurable interval so the UI always sees up-to-date source grouping and gather timing
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
| `dbg` | Debug (H2) |
| `dbg-dev` | Debug against dev database |
| `dbg-pdn` | Debug against production database |

```bash
# Run locally (H2, debug)
java -jar target/MiddleTier-Backup-*.jar

# Run with a specific profile
java -jar target/MiddleTier-Backup-*.jar --spring.profiles.active=pdn
```

In production the service runs as a systemd unit (`middletier-backup.service`) on port **12013**, behind a single **nginx** reverse proxy.

A `docker-compose-dev.yml` is provided for running a local development stack.

## Configuration

All custom properties are under the `backup.*` namespace. Key properties:

| Property | Description | Default |
|---|---|---|
| `backup.enabled` | Enable/disable scheduled backups | `false` |
| `backup.schedule` | Cron expression for the backup schedule | `0 20 0/1 * * ?` |
| `backup.gather-enabled` | Enable file system gather on schedule | `false` |
| `backup.gather-schedule` | Cron expression for the gather | `0 0 0 * * ?` |
| `backup.summary-refresh-hours` | How often (hours) the summary is rebuilt on a schedule | `4` |
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

All endpoints are versioned under `/api/v1`. The API is self-documented via Swagger UI at `/swagger-ui.html` when the service is running.

### Endpoints by resource

#### Actions (`/api/v1`)

| Method | Path | Description |
|---|---|---|
| GET | `/actions` | List pending synchronisation actions |
| POST | `/actions` | Confirm a pending action |
| GET | `/actions/confirmed` | List confirmed actions |
| POST | `/actions/email` | Send action summary email |
| GET | `/summary` | Current backup summary (SSE stream also at `/events/summary`) |
| GET | `/ignored` | List files permanently excluded from synchronisation |

#### Files (`/api/v1`)

| Method | Path | Description |
|---|---|---|
| GET | `/files` | List all tracked files |
| GET | `/files/detail?id=` | Get metadata for a single file |
| DELETE | `/file?id=` | Delete a tracked file record |
| GET | `/files/image?id=` | Serve file as JPEG image |
| GET | `/files/video?id=` | Serve file as video stream |
| GET | `/files/video-thumbnail?id=` | Serve video thumbnail as JPEG |
| GET | `/files/download?id=` | Download the original file |
| GET | `/files/search` | Search tracked files (simple) |
| PUT | `/files/date` | Update the date metadata on a file |
| PUT | `/files/location` | Update the location metadata on a file |
| POST | `/gather` | Trigger a file system gather |
| POST | `/sync/run` | Trigger a synchronisation run |
| POST | `/duplicates` | Find duplicate files |
| POST | `/hierarchy` | Get directory hierarchy |
| POST | `/files/refresh` | Refresh file metadata |
| PUT | `/files/expire` | Expire stale file records |
| GET | `/events/files` | SSE stream of file system events |

#### Search (`/api/v1`)

| Method | Path | Description |
|---|---|---|
| POST | `/backup/search` | Search backed-up files by filename glob, date range, size range, labels, GPS bounding-box, and expiry range; results are paged |

Request body fields: `filename`, `dateFrom`, `dateTo`, `sizeMin`, `sizeMax`, `expiryFrom`, `expiryTo`, `labels`, `location` (south/west/north/east), `page`, `pageSize`.

#### Import pipeline (`/api/v1/import`)

| Method | Path | Description |
|---|---|---|
| GET | `/` | List import sources |
| DELETE | `/file` | Delete a file from the import staging area |
| DELETE | `/ignored` | Delete all ignored import files |
| DELETE | `/active-photos` | Delete active photos from staging |
| POST | `/photos` | Trigger photo import |
| DELETE | `/confirmed` | Clear confirmed import files |
| POST | `/file/un-ignore` | Un-ignore a specific file |
| POST | `/un-ignore` | Un-ignore all files |
| POST | `/file/recipe` | Set recipe for a file |
| POST | `/file/backup` | Back up a file from import |
| POST | `/file/destination` | Set destination for a file |
| GET | `/events/files` | SSE stream of import file events |
| DELETE | `/data` | Clear all import data |
| DELETE | `/cache` | Clear import cache |
| GET | `/file/image?name=` | Serve import file as JPEG |
| GET | `/file/video?name=` | Serve import file as video |

#### Configuration (`/api/v1`)

| Method | Path | Description |
|---|---|---|
| GET/POST/PUT/DELETE | `/locations` | Physical storage location configuration |
| GET/POST/PUT/DELETE | `/sources` | Source directory configuration |
| GET/POST/PUT/DELETE | `/sync` | Synchronisation pair configuration |
| GET/POST/PUT/DELETE | `/classifications` | File classification rules (includes `image`, `video`, `browser` flags) |
| GET/POST/DELETE | `/labels` | User-defined file labels |
| GET/POST/PUT | `/prints` | Print selection management |
| GET | `/prints/sizes` | List available print sizes |
| POST | `/prints/unselect` | Unselect all prints |
| DELETE | `/prints/{fileId}/{sizeId}` | Remove a specific print selection |
| DELETE | `/prints` | Remove all print selections |
| POST | `/prints/generate` | Generate print output |
| GET | `/version` | Service version |

#### Backup jobs (`/api/v1`)

| Method | Path | Description |
|---|---|---|
| GET | `/backup/status/jobs` | List all backup job run records (ordered by run date) |
| GET | `/backup/status/jobs/{backupId}` | History for a specific backup job (latest `limit` runs, default 10) |
| POST | `/backup-jobs/run` | Trigger an immediate backup run |

#### Hardware (`/api/v1/hardware`)

| Method | Path | Description |
|---|---|---|
| GET/POST/PUT/DELETE | `/` | Manage network hardware entries (keyed by MAC address) |

### Actuator

Spring Boot Actuator is fully exposed. The health endpoint includes service-specific details:

```
GET /actuator/health
```

## Database Migrations

Schema is managed by Liquibase. Changelogs are in `src/main/resources/db/changelog/`. A separate set of H2-compatible changelogs exists under `db/changelog/h2/` for local and test use.

## CI/CD

The project builds and deploys via GitHub Actions on a self-hosted runner (`backup-prod` for Release branch, `backup-dev` for all other branches).

The pipeline:
1. Installs Maven and `exiftool` (`libimage-exiftool-perl`)
2. Runs `mvn verify` — unit tests (H2) + integration tests (TestContainers/MySQL)
3. Runs SonarCloud analysis
4. Builds and pushes a Docker image to the Nexus registry
5. Deploys via `docker compose`

### Known CI gotchas

- **`exiftool` must be installed** — the runner install step includes `libimage-exiftool-perl`; without it EXIF-related tests and production code will fail.
- **Docker API version** — `src/test/resources/docker-java.properties` pins `api.version=1.44`. Without this, the docker-java client defaults to API 1.32 which modern Docker daemons reject with a 400 error. If integration tests fail with `client version 1.32 is too old`, check this file exists.
- **TestContainers requires `DOCKER_HOST`** — the workflow sets `DOCKER_HOST: unix:///var/run/docker.sock` and `TESTCONTAINERS_RYUK_DISABLED: "true"` on the Maven step.

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
  schedule/     Scheduled tasks (backup, gather, summary refresh)
  summary/      Summary model rebuilt on a configurable schedule
  util/         Utilities (geo-coordinates, image metadata, file search)
```
