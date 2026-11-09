package com.studymate.reservation;

import com.jayway.jsonpath.JsonPath;
import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.global.domain.ActivityRegion;
import com.studymate.global.exception.*;
import com.studymate.meeting.domain.Meeting;
import com.studymate.member.domain.*;
import com.studymate.reservation.application.service.*;
import com.studymate.reservation.domain.branch.Branch;
import com.studymate.reservation.domain.room.Room;
import com.studymate.reservation.domain.reservation.*;
import com.studymate.study.domain.Study;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real commits, locks and rollback on a disposable database; never the development datasource. */
@EnabledIfEnvironmentVariable(named = "RESERVATION_PG_TEST", matches = "true")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:15440/reservation_qr_test?ApplicationName=reservation-qr-test",
    "spring.datasource.username=postgres", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "studymate.meeting.deadlines.enabled=false",
    "jwt.secret=reservation-qr-test-only-secret-012345678901234567890123456789",
    "jwt.access-token-expiration-seconds=3600"
})
class ReservationQrPostgresIntegrationTest {
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired ReservationQrCodeService qr;
    @Autowired ReservationQrManagementService management;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired WebApplicationContext context;
    UUID leader, member, studyId, reservationId, leaderSeat, memberSeat;
    MockMvc mvc;
    final ZoneId korea = ZoneId.of("Asia/Seoul");
    final LocalDateTime startAt = LocalDateTime.of(2030, 1, 10, 13, 0);
    org.mockito.MockedStatic<LocalDateTime> clock;

    @BeforeEach void fixture() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith(
                    "jdbc:postgresql://127.0.0.1:15440/reservation_qr_test"));
        }
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clock = org.mockito.Mockito.mockStatic(LocalDateTime.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        clock.when(() -> LocalDateTime.now(korea)).thenReturn(startAt.minusDays(2));
        tx.executeWithoutResult(status -> {
            leader = member(); member = member();
            Study study = new Study("QR study", "test", leader, 10, ActivityRegion.SEOUL_GANGNAM);
            em.persist(study); studyId = study.getId();
            Branch branch = new Branch("branch", "address", leader); em.persist(branch);
            Room room = new Room(branch.getId(), "room", 10, 1, leader); em.persist(room);
            LocalDateTime start = startAt;
            Meeting meeting = new Meeting(studyId, branch.getId(), "meeting", "test", 10, start, start.plusHours(3), leader);
            em.persist(meeting);
            Reservation reservation = new Reservation(meeting.getId(), room.getId(), start, start.plusHours(3), 1, leader);
            em.persist(reservation); reservationId = reservation.getId(); meeting.confirm(leader);
            ReservationParticipant first = new ReservationParticipant(reservationId, leader, 1, leader);
            ReservationParticipant second = new ReservationParticipant(reservationId, member, 1, leader);
            em.persist(first); em.persist(second); leaderSeat = first.getId(); memberSeat = second.getId();
            em.persist(new ReservationQrManagement(reservationId, leader));
        });
        clock.when(() -> LocalDateTime.now(korea)).thenReturn(startAt.plusMinutes(1));
    }

    @AfterEach void releaseClock() { if (clock != null) clock.close(); }

    UUID member() {
        Member result = new Member(UUID.randomUUID() + "@example.com", "encoded", "tester", ActivityRegion.SEOUL_GANGNAM);
        em.persist(result); return result.getId();
    }
    String base() { return "/api/studies/" + studyId + "/reservations/" + reservationId + "/attendance"; }
    RequestPostProcessor as(UUID id) {
        var principal = new MemberPrincipal(id, "test@example.com", MemberRole.USER);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    String body(String key, String value) { return "{\"" + key + "\":\"" + value + "\"}"; }
    String issuedToken() {
        management.open(studyId, reservationId, leader);
        return qr.issue(studyId, reservationId, leader).token();
    }
    long attendanceCount(UUID seat) {
        return jdbc.queryForObject("select count(*) from reservation_attendance where reservation_participant_id=?", Long.class, seat);
    }
    Object usedAt(String token) throws Exception {
        String hash = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return jdbc.queryForObject("select used_at from reservation_attendance_grant where token_hash=?", (rs, n) -> rs.getTimestamp(1), hash);
    }

    @Test void httpOpenIssueScanCheckInRescanCheckOutAndRead() throws Exception {
        mvc.perform(post(base() + "/qr/open").with(as(leader))).andExpect(status().isNoContent());
        mvc.perform(post(base() + "/operator/check-in").with(as(leader))).andExpect(status().isNoContent());
        String issueJson = mvc.perform(post(base() + "/qr").with(as(leader)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String shared = JsonPath.read(issueJson, "$.token");
        String scanJson = mvc.perform(post(base() + "/qr/scan").with(as(member))
                .contentType("application/json").content(body("token", shared)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.action").value("CHECK_IN"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(0, attendanceCount(memberSeat));
        String grant = JsonPath.read(scanJson, "$.token");
        mvc.perform(post(base() + "/check-in").with(as(member)).contentType("application/json")
                .content(body("grantToken", grant))).andExpect(status().isNoContent());
        assertEquals(1, attendanceCount(memberSeat));
        assertNotNull(usedAt(grant));
        scanJson = mvc.perform(post(base() + "/qr/scan").with(as(member))
                .contentType("application/json").content(body("token", shared)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.action").value("CHECK_OUT"))
                .andReturn().getResponse().getContentAsString();
        String out = JsonPath.read(scanJson, "$.token");
        mvc.perform(post(base() + "/check-out").with(as(member)).contentType("application/json")
                .content(body("grantToken", out))).andExpect(status().isNoContent());
        assertNotNull(usedAt(out));
        mvc.perform(get(base() + "/me").with(as(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedInAt").exists()).andExpect(jsonPath("$.checkedOutAt").exists())
                .andExpect(jsonPath("$.sessions.length()").value(3));
        mvc.perform(post(base() + "/qr/scan").with(as(member)).contentType("application/json")
                .content(body("token", shared))).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist()).andExpect(jsonPath("$.checkedOutAt").exists());
    }

    @Test void httpRejectsUnauthenticatedMissingTokenWrongOwnerAndOperatorBypass() throws Exception {
        String shared = issuedToken();
        String grant = qr.scan(studyId, reservationId, member, shared).token();
        mvc.perform(post(base() + "/qr/scan").contentType("application/json").content(body("token", shared)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(base() + "/check-in").with(as(member))).andExpect(status().isBadRequest());
        mvc.perform(post(base() + "/check-in").with(as(leader)).contentType("application/json").content(body("grantToken", grant)))
                .andExpect(status().isForbidden());
        mvc.perform(post(base() + "/check-out").with(as(member)).contentType("application/json").content(body("grantToken", grant)))
                .andExpect(status().isForbidden());
        mvc.perform(post(base() + "/operator/check-in").with(as(member))).andExpect(status().isForbidden());
        assertNull(usedAt(grant)); assertEquals(0, attendanceCount(memberSeat));
    }

    @Test void handoverRevokesDirectAccessButKeepsOldQrAndPersonalGrant() throws Exception {
        String shared = issuedToken();
        qr.checkInAsOperator(studyId, reservationId, leader);
        String in = qr.scan(studyId, reservationId, member, shared).token();
        qr.checkInWithGrant(studyId, reservationId, member, in);
        String previousOperatorOut = qr.scan(studyId, reservationId, leader, shared).token();
        mvc.perform(post(base() + "/qr/handover").with(as(leader)).contentType("application/json")
                .content(body("nextParticipantId", memberSeat.toString()))).andExpect(status().isNoContent());
        assertThrows(ForbiddenException.class, () -> qr.issue(studyId, reservationId, leader));
        assertThrows(ForbiddenException.class, () -> qr.checkOutAsOperator(studyId, reservationId, leader));
        assertNotNull(qr.issue(studyId, reservationId, member).token());
        assertEquals(AttendanceAction.CHECK_OUT, qr.scan(studyId, reservationId, member, shared).action());
        assertNotNull(qr.checkOutWithGrant(studyId, reservationId, leader, previousOperatorOut).getCheckedOutAt());
    }

    @Test void delegateHttpRequiresLeaderAndCannotChangeAfterOpening() throws Exception {
        mvc.perform(patch(base() + "/qr/delegate").with(as(member)).contentType("application/json")
                .content(body("delegateParticipantId", memberSeat.toString()))).andExpect(status().isForbidden());
        mvc.perform(patch(base() + "/qr/delegate").with(as(leader)).contentType("application/json")
                .content(body("delegateParticipantId", memberSeat.toString()))).andExpect(status().isNoContent());
        mvc.perform(post(base() + "/qr/open").with(as(member))).andExpect(status().isNoContent());
        mvc.perform(patch(base() + "/qr/delegate").with(as(leader)).contentType("application/json")
                .content(body("delegateParticipantId", leaderSeat.toString()))).andExpect(status().isConflict());
    }

    @Test void persistedPersonalGrantSurvivesQrExpiryButStopsAtItsOwnExpiry() throws Exception {
        String shared = issuedToken();
        clock.when(() -> LocalDateTime.now(korea)).thenReturn(startAt.plusMinutes(1).plusSeconds(59));
        var personal = qr.scan(studyId, reservationId, member, shared);
        clock.when(() -> LocalDateTime.now(korea)).thenReturn(startAt.plusMinutes(2));
        assertThrows(ConflictException.class, () -> qr.scan(studyId, reservationId, member, shared));
        qr.checkInWithGrant(studyId, reservationId, member, personal.token());
        String fresh = qr.issue(studyId, reservationId, leader).token();
        var out = qr.scan(studyId, reservationId, member, fresh);
        clock.when(() -> LocalDateTime.now(korea)).thenReturn(out.expiresAt());
        assertThrows(ConflictException.class, () -> qr.checkOutWithGrant(studyId, reservationId, member, out.token()));
        assertNull(usedAt(out.token()));
        assertNull(jdbc.queryForObject("select checked_out_at from reservation_attendance where reservation_participant_id=?",
                (rs, n) -> rs.getTimestamp(1), memberSeat));
    }

    @Test void checkInDeadlineDoesNotExtendWithGrantAndFailureLeavesGrantUnused() throws Exception {
        management.open(studyId, reservationId, leader);
        clock.when(() -> LocalDateTime.now(korea)).thenReturn(startAt.plusHours(3).minusMinutes(10).minusSeconds(1));
        String shared = qr.issue(studyId, reservationId, leader).token();
        var grant = qr.scan(studyId, reservationId, member, shared);
        clock.when(() -> LocalDateTime.now(korea)).thenReturn(startAt.plusHours(3).minusMinutes(10));
        assertThrows(ConflictException.class, () -> qr.checkInWithGrant(studyId, reservationId, member, grant.token()));
        assertEquals(0, attendanceCount(memberSeat)); assertNull(usedAt(grant.token()));
    }

    List<String> race(Supplier<String> first, Supplier<String> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (Supplier<String> operation : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("barrier timeout");
                    try (var workerClock = org.mockito.Mockito.mockStatic(LocalDateTime.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
                        workerClock.when(() -> LocalDateTime.now(korea)).thenReturn(startAt.plusMinutes(1));
                        try { return operation.get(); } catch (ConflictException e) { return "conflict"; }
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        } finally { start.countDown(); executor.shutdownNow(); }
    }

    @Test void concurrentOpeningChoosesExactlyOneOperator() throws Exception {
        management.changeDelegate(studyId, reservationId, leader, memberSeat);
        var results = race(() -> { management.open(studyId, reservationId, leader); return "leader"; },
                () -> { management.open(studyId, reservationId, member); return "member"; });
        assertEquals(1, results.stream().filter("conflict"::equals).count());
        UUID expected = results.contains("leader") ? leaderSeat : memberSeat;
        assertEquals(expected, jdbc.queryForObject("select operator_participant_id from reservation_qr_management where reservation_id=?", UUID.class, reservationId));
    }

    @Test void concurrentGrantUseRecordsExactlyOneAttendanceAndPreservesFirstTime() throws Exception {
        String grant = qr.scan(studyId, reservationId, member, issuedToken()).token();
        Supplier<String> operation = () -> { qr.checkInWithGrant(studyId, reservationId, member, grant); return "ok"; };
        var results = race(operation, operation);
        assertEquals(1, results.stream().filter("ok"::equals).count());
        assertEquals(1, results.stream().filter("conflict"::equals).count());
        assertEquals(1, attendanceCount(memberSeat)); assertNotNull(usedAt(grant));
        Object original = jdbc.queryForObject("select checked_in_at from reservation_attendance where reservation_participant_id=?", (rs, n) -> rs.getTimestamp(1), memberSeat);
        assertThrows(ConflictException.class, () -> qr.checkInWithGrant(studyId, reservationId, member, grant));
        assertEquals(original, jdbc.queryForObject("select checked_in_at from reservation_attendance where reservation_participant_id=?", (rs, n) -> rs.getTimestamp(1), memberSeat));
    }

    @Test void failedGrantFlushRollsBackAttendanceAndAllowsRetry() throws Exception {
        String grant = qr.scan(studyId, reservationId, member, issuedToken()).token();
        // Deliberately force the grant UPDATE to fail after attendance has been persisted.
        jdbc.execute("alter table reservation_attendance_grant add constraint qr_test_reject_use check (reservation_participant_id <> '"
                + memberSeat + "'::uuid or used_at is null)");
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> qr.checkInWithGrant(studyId, reservationId, member, grant));
            assertEquals(0, attendanceCount(memberSeat)); assertNull(usedAt(grant));
        } finally {
            jdbc.execute("alter table reservation_attendance_grant drop constraint qr_test_reject_use");
        }
        qr.checkInWithGrant(studyId, reservationId, member, grant);
        assertEquals(1, attendanceCount(memberSeat)); assertNotNull(usedAt(grant));
    }
}
