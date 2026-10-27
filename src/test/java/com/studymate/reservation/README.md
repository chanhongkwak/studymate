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
