# Integrated backend verification recheck — 2026-11-12

Checked the clean integrated `feat/architecture-phase1` worktree at `c446e58`
in `/run/media/yacine/yassine/DMS/architecture-foundations`.

## Newly verified

- Java runtime 21.0.13.0.3 and Maven 3.9.12 are now installed on PATH.
- The user Maven cache now contains the application dependencies. Copied that
  cache into writable `/tmp/dms-backend-recheck-maven` for offline verification.
- **All 37 application source files and 7 test source files compile** targeting
  Java 21, including MapStruct-generated mappings. The installed Java 21 runtime
  has no javac; compilation used IntelliJ's bundled compiler with release 21.
- **17 JUnit tests pass on Java 21**: ProjectRequestParserTest (9),
  ProjectActorTest (5), PasswordPolicyTest (3). Executed the compiled classes
  directly with cached JUnit Jupiter 5.12.2 engine and Platform 1.12.2, because
  the Maven test-plugin cache remains incomplete. This is not a Maven verify pass.
- **13 offline contract checks pass**.
- Compose config validates with an explicitly supplied validation-only password;
  the worktree has no configured POSTGRES_PASSWORD for actual local startup.
- `git diff --check` passes; the integrated source tree was not modified.

Successful compile command, from integrated `backend/`:

```bash
./mvnw -o -Dmaven.repo.local=/tmp/dms-backend-recheck-maven \
  -Dmaven.compiler.fork=true \
  -Dmaven.compiler.executable=/snap/intellij-idea/154/jbr/bin/javac test-compile
```

The temporary JUnit engine runner used the Maven-compiled target/classes and
target/test-classes plus cached dependency jars. No substitute application or
test stubs were used. The runner is `/tmp/dms-recheck-unit/DmsRecheckJUnit.java`;
temporary caches/runners are environment-local and are not repository tooling.

## Remaining blockers

- DNS for repo.maven.apache.org and registry-1.docker.io still fails from this
  execution environment. Normal online Maven verification fails parent resolution
  when using an empty cache.
- Offline `verify` reaches the test phase but fails loading Surefire 3.5.6:
  missing `org/apache/maven/plugin/surefire/SurefireReportParameters`. Its cached
  POM/dependency metadata is incomplete. The jar plugin POM is also missing.
- Docker API access to `/var/run/docker.sock` is still denied. No PostgreSQL
  connection, Flyway execution, HTTP session workflow, or PostgreSQL integration
  test has been verified.

Next acceptance gate remains a complete `./mvnw verify` with a full JDK/compiler,
complete Maven dependencies and accessible Docker, followed by frontend integration.
The earlier statements that the source had not compiled are historical and are
superseded by this recheck; PostgreSQL/runtime acceptance remains open.
