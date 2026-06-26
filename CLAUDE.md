# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Full build including integration tests
mvn verify

# Build only (skip all tests)
mvn package -Dskip.surefire.tests=true -DskipITs

# Unit tests only
mvn test

# Run a single test class
mvn test -Dtest=TestBasicCRUD

# Run a single test method
mvn test -Dtest=TestBasicCRUD#methodName

# SonarCloud quality analysis
mvn sonar:sonar
```

JaCoCo coverage reports are written to `target/site/jacoco/` (unit) and `target/site/jacoco-it/` (integration).

## Running the Application

```bash
# Local/debug mode — H2 in-memory database
java -jar target/MiddleTier-Backup-*.jar

# Production — MySQL, port 12013
java -jar target/MiddleTier-Backup-*.jar --spring.profiles.active=pdn
```

Spring profiles: `dev`, `pdn` (production), `dbg`, `dbg-dev`, `dbg-pdn`. Config files live in `src/main/resources/config/application-<profile>.yml`.

## Architecture

**Spring Boot 3 REST microservice** for backup management, directory synchronization, and media import. Uses MySQL in production and H2 for unit tests. Schema managed by Liquibase (`src/main/resources/db/changelog/`).

### Layer Structure

```
control/        → REST controllers (URL → manager delegation)
manager/        → Business logic (core orchestration)
dataaccess/     → Spring Data JPA repositories
data/           → JPA entities
dto/            → API request/response objects
filetree/       → File tree abstraction (compare/, database/, realworld/)
schedule/       → Scheduled tasks (backup, file gather)
type/           → Enums and domain types
util/           → EXIF, geo, file search utilities
```

### Key Managers

| Manager | Responsibility |
|---|---|
| `BackupManager` | Orchestrates `mysqldump`-based database/file backups |
| `SynchronizeManager` | Compares and syncs source/destination directory pairs |
| `ImportManager` | Drives the three-stage media import pipeline |
| `FileSystem` | File I/O, hashing, copying, deletion |
| `FileSystemObjectManager` | Persists file/directory metadata to DB |
| `ActionManager` | Queues and confirms pending file actions |
| `DuplicateManager` | Detects duplicate files across locations |

### REST API URL Pattern

- External endpoints: `/jbr/ext/backup/**`
- Internal endpoints: `/jbr/int/backup/**`
- Hardware registry: `/jbr/ext/hardware/**`

### Import Pipeline

The import system (`manager/importing/`) is multi-threaded (default 5 threads, configurable via `backup.import-threads`). It processes media files in discrete steps located in `manager/importing/step/`. ffmpeg and thumbnail extraction commands are configured via `backup.ffmpeg-command` and `backup.vid-to-image-command`.

### Testing Approach

- **Unit tests** use H2 in-memory database and MockMvc
- **Integration tests** (`src/test/java/.../integration/`) use TestContainers with a real MySQL instance and GreenMail for SMTP
- `WebTester` is the base class providing MockMvc setup for controller tests
