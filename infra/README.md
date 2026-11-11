# Local infrastructure

Run commands from the repository/worktree root. Requirements: JDK 21, Maven 3.6.3+ on PATH (or MAVEN_COMMAND set to the executable), Docker Engine with Compose and accessible socket, access to Maven Central and the PostgreSQL image registry. One PostgreSQL 16 database serves the backend. The canonical Compose file stays at the repository root to preserve `docker compose up -d db` compatibility; infra holds run helpers instead of a second Compose/database definition.

```bash
cp .env.example .env
# Edit .env: choose a local POSTGRES_PASSWORD before starting.
docker compose config --quiet
docker compose up -d --wait db
./infra/scripts/backend-local.sh
```

The helper sources the operator-controlled local .env (shell-compatible assignments; quote special characters), exports its values, enters backend and runs Spring Boot. Compose reads .env itself; Spring Boot does not automatically load it. For direct Maven runs, export variables explicitly. Standard datasource URL/username/password variables override local POSTGRES_* settings for managed PostgreSQL. No .env or real data is committed.

In another terminal:

```bash
curl --fail http://127.0.0.1:8080/actuator/health
curl --fail http://127.0.0.1:8080/actuator/health/liveness
curl --fail http://127.0.0.1:8080/actuator/health/readiness
cd backend
./mvnw verify
```

Health is Boot Actuator, not a custom business controller. Readiness includes the database; liveness does not. Details are hidden. Database and backend bind to loopback by default. Set SERVER_ADDRESS explicitly for a container/deployment. Agent 2's integrated security requires a validated JWT plus a provisioned active application user and database roles/project access. Without a configured trusted issuer, all business tokens are rejected while health remains public. Set SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI, optionally SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI, and EXPORTDOCS_SECURITY_AUDIENCE. See `../backend/PROJECT-IMPLEMENTATION-HANDOFF.md` for provisioning and authenticated API examples. This repository does not supply an identity provider.

`backend/mvnw` is a small installed-Maven launcher, **not** an Apache downloading wrapper. This makes the offline prerequisite explicit and retains the repository's `./mvnw verify` command. Boot's parent manages plugin/dependency versions. Compilation target is Java 21. `verify` runs JUnit/Testcontainers tests through Failsafe; Docker absence fails verification, with no disabled/skipped-container success gate. Testcontainers starts an isolated PostgreSQL and does not modify the local Compose database. There is no H2 substitute.

Flyway applies V1 transactionally at startup; Hibernate uses ddl-auto=validate and never creates tables. Flyway clean/baseline are disabled. On an existing unmanaged schema, resolve migration history explicitly; do not turn on automatic baselining. New migrations must be coordinated and use the next available version; never edit a migration already applied to any shared database.

```bash
# Inspect migration history after the backend has started:
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT installed_rank,version,description,success FROM flyway_schema_history ORDER BY installed_rank"'
# Stop while retaining data:
docker compose down
```

The named pgdata volume persists. Do not use `down -v` on data you need. The Compose account owns the local development database; production deployment must separate migration ownership from application DML privileges and restrict audit/document history mutation.

Optional offline contract checks use Python 3 plus PyYAML/jsonschema (development tools only):

```bash
python3 infra/scripts/check-contract.py
bash -n infra/scripts/backend-local.sh
sh -n backend/mvnw
```

Full verification status and environment limitations are recorded in `docs/architecture/VALIDATION.md`. Frontend checks belong to its agent; no Angular files are edited by the foundation slice.
