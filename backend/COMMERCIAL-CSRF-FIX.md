# Commercial test CSRF transport fix

The 2026-11-12 host verification executed all 121 PostgreSQL integration cases.
CommercialSetupApiIT had 31 failures out of 32: its shared write helper used
Spring Security's `csrf()` postprocessor, which added `_csrf` as a request
parameter. The strict commercial controllers rejected that parameter before
the business operation, including before the grant-revocation test's lock latch.
The other 89 integration cases passed.

CommercialTestRequests now sends a matching `XSRF-TOKEN` cookie and raw
`X-XSRF-TOKEN` header, matching AccountSecurityConfiguration and the existing
account HTTP tests. Production security, query validation, and integration
assertions are unchanged.

CommercialCsrfTransportTest exercises the real security filter chain, parsers,
and all seven commercial write routes with mocked business services. Its 22
cases check successful cookie/header transport, rejection of unknown query
parameters (including `_csrf`), and rejection of absent/mismatched CSRF tokens.
The old helper reproduced seven failing positive-route cases before the fix.

After the fix, all **72 unit/MVC tests passed**, with zero failures, errors, or
skips, using Java 21 and the current IntelliJ compiler with release 21. All
integration sources also compiled. Local logs: `/tmp/dms-csrf-transport-before.log`
and `/tmp/dms-csrf-transport-after.log`.

Full PostgreSQL acceptance after this change remains pending: the sandbox
cannot access Docker. Run `./verify.sh` from `architecture-foundations/backend`
in the ordinary host terminal. The expected suite is 72 unit/MVC plus 121
PostgreSQL cases, **193 total**. Do not treat the local MVC result as full
commercial acceptance.
