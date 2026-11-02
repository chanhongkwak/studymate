# Reservation service PostgreSQL integration tests

Run only against a disposable PostgreSQL 16 instance. The test class explicitly
uses `127.0.0.1:55437/reservation_test` and `create-drop`. Never substitute the
development database. Execution is opt-in via `RESERVATION_PG_TEST=true`.

PowerShell example (choose an unused container name):

```powershell
docker run --detach --name studymate-reservation-test -e POSTGRES_DB=reservation_test -e POSTGRES_HOST_AUTH_METHOD=trust -p 127.0.0.1:55437:5432 postgres:16
docker exec studymate-reservation-test pg_isready -U postgres -d reservation_test
$env:JAVA_HOME='C:\Users\User\.jdks\ms-21.0.10'
$env:RESERVATION_PG_TEST='true'
.\gradlew.bat test --tests com.studymate.reservation.ReservationPostgresIntegrationTest --rerun-tasks --console=plain
docker rm --force --volumes studymate-reservation-test
Remove-Item Env:RESERVATION_PG_TEST
```

Wait for `pg_isready` to report readiness before running Gradle. Use a local Java
21 installation. No production source changes or dependency additions are needed.
The example database is unauthenticated and bound only to loopback; remove it
after use, including after test failure.

## Verified run: 2026-10-27 (Asia/Seoul)

31 tests, 0 failures, 0 errors, 0 skipped, PostgreSQL 16 and Java 21.

Coverage includes persisted allocation/debits, zero-cost participants, retries,
authorization, majority/capacity/branch/room checks, max share, partial-debit and
late-insert rollback, exact 24-hour boundaries, deadline crossing after a real
flush, overlapping/adjacent reservations, concurrent duplicate requests and
double bookings, room/branch changes while a reservation waits, and paying/absent
leader deletion ordering.

Tests use real transactions and PostgreSQL. Boundary tests replace only the
thread-local Java clock through Mockito; the deadline-crossing test spies on the
repository to advance that clock after its real flush. Lock-order tests observe
`pg_stat_activity.wait_event_type = 'Lock'` before releasing the blocking
transaction. Fixture funding is direct wallet credit, not a real payment.

This run does not constitute HTTP/controller testing, actual payment validation,
exhaustive input testing, or a rerun of the pre-existing wallet suite. Default
runs without the opt-in environment variable skip this class.

Reports: `build/reports/tests/test/index.html` and
`build/test-results/test/TEST-com.studymate.reservation.ReservationPostgresIntegrationTest.xml`.

## Reservation detail and list tests (2026-10-28)

`ReservationReadPostgresIntegrationTest` uses a separate disposable database:
`127.0.0.1:55438/reservation_read_test`. It never uses the development datasource.
The opt-in variable is also `RESERVATION_PG_TEST=true`; without it the class is skipped.
Each test rolls back its fixtures, and the Spring context uses `create-drop`.

```powershell
docker run --detach --name studymate-reservation-read-test -e POSTGRES_DB=reservation_read_test -e POSTGRES_HOST_AUTH_METHOD=trust -p 127.0.0.1:55438:5432 postgres:16
docker exec studymate-reservation-read-test pg_isready -U postgres -d reservation_read_test
$env:JAVA_HOME='C:\Users\User\.jdks\ms-21.0.10'
$env:RESERVATION_PG_TEST='true'
try {
    .\gradlew.bat test --tests com.studymate.reservation.ReservationReadPostgresIntegrationTest --rerun-tasks --console=plain
} finally {
    docker rm --force --volumes studymate-reservation-read-test
    Remove-Item Env:RESERVATION_PG_TEST
}
```

Use an unused container name and wait for `pg_isready` before running Gradle.
Never substitute the development database URL. No separately running application
server is needed. MockMvc uses the real web context, security filter chain,
controller, service and PostgreSQL repositories. Authenticated tests inject a
`MemberPrincipal` through Spring Security test support; JWT issuance and token
signature verification are outside this suite's scope.

Verified: **35 tests, 0 failures, 0 errors, 0 skipped**, PostgreSQL 16 and Java 21.
Coverage includes non-participant study-member access, stored participants with
zero amounts, canceled reservations and inactive facilities, outsider and
membership-state denial, completed memberships in ended studies, latest
membership history, deleted/missing resources, study isolation, start-time/UUID
sorting, count queries, pagination, empty pages, and HTTP paths/defaults/response
shape plus 400/401/403/404 responses. Fixtures use direct persistence to isolate
read behavior; the reservation creation suite was not rerun for this change.

XML report:
`build/test-results/test/TEST-com.studymate.reservation.ReservationReadPostgresIntegrationTest.xml`.

## Cancellation and refund tests (2026-10-28)

`ReservationCancelPostgresIntegrationTest` is opt-in via `RESERVATION_PG_TEST=true`
and explicitly uses the disposable `127.0.0.1:55439/reservation_cancel_test`
database with `create-drop`. Never replace this URL with a development database.
Fixtures create real reservations through the creation service; initial wallet
funding is test-only direct credit. Tests use real commits/rollbacks, and cleanup
deletes fixtures only in this dedicated database.

```powershell
docker run --detach --name studymate-reservation-cancel-test -e POSTGRES_DB=reservation_cancel_test -e POSTGRES_HOST_AUTH_METHOD=trust -p 127.0.0.1:55439:5432 postgres:16
docker exec studymate-reservation-cancel-test pg_isready -U postgres -d reservation_cancel_test
$env:JAVA_HOME='C:\Users\User\.jdks\ms-21.0.10'
$env:RESERVATION_PG_TEST='true'
try {
    .\gradlew.bat test --tests com.studymate.reservation.ReservationCancelPostgresIntegrationTest --rerun-tasks --console=plain
} finally {
    docker rm --force --volumes studymate-reservation-cancel-test
    Remove-Item Env:RESERVATION_PG_TEST
}
```

Choose an unused container name, and wait for `pg_isready` before running Gradle.
No separately running application server is required.

Verified: **32 tests, 0 failures, 0 errors, 0 skipped**, PostgreSQL 16 / Java 21.
Coverage includes exact per-member refunds, zero shares, absent/current/former
leaders, deleted/departed recipients, inactive facilities, input/permission
checks, repeated cancellation, read-history preservation, final 24-hour boundary
at -1/0/+1 nanoseconds, deadline crossing after real flush, missing debit and
database constraint failure rollback, study end/delete guards and release after
cancellation, old meeting cancellation bypass prevention, and room-slot reuse.

Concurrency tests observe real PostgreSQL lock waits: duplicate cancellations,
leader deletion while waiting for a room (including a non-participant leader),
and study termination waiting for cancellation. MockMvc covers successful PATCH,
401/403, and deadline 409 with real service/repository behavior. Authentication
is injected through Spring Security test support, so JWT issuance/signature
validation is not covered. Java time checks use thread-local mocked time; SQL
timestamp boundary checks use microsecond precision to match PostgreSQL.

Only this cancellation suite was executed for this change; existing full
creation/read suites were not rerun. No production code was changed by the test
work. XML report:
`build/test-results/test/TEST-com.studymate.reservation.ReservationCancelPostgresIntegrationTest.xml`.
