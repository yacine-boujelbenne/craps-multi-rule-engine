# Run the complete backend verification

From the backend directory in the worktree you want to check:

```bash
./verify.sh
```

You can also invoke the script by absolute path from any directory. It locates its
own backend, checks Docker access, chooses a Java compiler and runs `./mvnw verify`.
It preserves Maven's exit status. It does not skip tests or change Docker permissions.

Use Java 21 for the supported test runtime. The script tries an explicit
JAVAC_COMMAND, JAVA_HOME/bin/javac, javac on PATH, then the current IntelliJ Snap
compiler. The build still targets Java 21 through pom.xml. The Snap fallback uses
`current`, so it does not depend on a particular installed revision.

```bash
# Select another compiler, if needed:
JAVAC_COMMAND=/path/to/jdk/bin/javac ./verify.sh

# Optional: use an already complete dependency cache offline:
./verify.sh -o
```

The existing Maven launcher accepts MAVEN_COMMAND when Maven is not on PATH.
Online verification needs Maven Central and container-registry access. Docker must
be accessible to the process running the script; a working Docker terminal does
not imply an agent sandbox has the same access.

Testcontainers starts isolated PostgreSQL databases. Verification does not require
the persistent Compose database or its POSTGRES_PASSWORD, and it does not load the
local .env. Look for BUILD SUCCESS and zero failures/errors/skips in both
target/surefire-reports and target/failsafe-reports. Frontend/browser checks remain
a separate integration gate.
