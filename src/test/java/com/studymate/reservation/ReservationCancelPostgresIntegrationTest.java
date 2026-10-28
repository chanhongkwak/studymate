package com.studymate.reservation;

import com.studymate.global.domain.ActivityRegion;
import com.studymate.global.exception.*;
import com.studymate.member.domain.*;
import com.studymate.member.application.MemberService;
import com.studymate.meeting.domain.*;
import com.studymate.reservation.application.dto.request.ReservationCreateRequest;
import com.studymate.reservation.application.dto.response.ReservationResponse;
import com.studymate.reservation.application.service.ReservationService;
import com.studymate.reservation.domain.branch.*;
import com.studymate.reservation.domain.room.*;
import com.studymate.reservation.domain.reservation.*;
import com.studymate.study.domain.*;
import com.studymate.wallet.domain.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in disposable PostgreSQL only. Never change this URL to the development database. */
@EnabledIfEnvironmentVariable(named = "RESERVATION_PG_TEST", matches = "true")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55439/reservation_cancel_test?ApplicationName=reservation-cancel-test",
    "spring.datasource.username=postgres", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=reservation-test-only-secret-012345678901234567890123456789",
    "jwt.access-token-expiration-seconds=3600"
})
class ReservationCancelPostgresIntegrationTest {
    @Autowired com.studymate.study.application.service.StudyService studyService;
    @Autowired com.studymate.meeting.application.service.MeetingService meetingService;
    @Autowired org.springframework.web.context.WebApplicationContext webContext;
    @Autowired javax.sql.DataSource dataSource;
    ReservationResponse booked;

    void book() { booked = reserve(); }
    ReservationResponse cancel() { return service.cancel(studyId, booked.id(), leader); }
    void endStudy() {
        studyService.changeStatus(studyId, leader,
                new com.studymate.study.application.dto.request.StudyStatusUpdatedRequest(StudyStatus.ENDED));
    }
    long returns() {
        return em.createQuery("select count(t) from WalletTransaction t where t.walletTransactionType=:type", Long.class)
                .setParameter("type", WalletTransactionType.RESERVATION_RETURN).getSingleResult();
    }
    void assertCanceled() {
        tx(() -> {
            assertEquals(ReservationStatus.CANCELED, em.find(Reservation.class, booked.id()).getReservationStatus());
            assertEquals(MeetingStatus.CANCELED, em.find(Meeting.class, meetingId).getStatus());
            assertEquals(booked.participants().size(), count("ReservationParticipant"));
            assertEquals(booked.participants().stream().filter(p -> p.amount() > 0).count(), returns());
            for (UUID wallet : wallets.values()) assertEquals(100L, em.find(Wallet.class, wallet).getBalance());
            for (var p : booked.participants()) {
                if (p.amount() == 0) continue;
                var refund = em.createQuery("select t from WalletTransaction t where t.walletId=:wallet and t.walletTransactionType=:type", WalletTransaction.class)
                        .setParameter("wallet", wallets.get(p.memberId()))
                        .setParameter("type", WalletTransactionType.RESERVATION_RETURN).getSingleResult();
                assertEquals(p.amount(), refund.getAmount());
                assertEquals(booked.id(), refund.getReferenceId());
                assertEquals(leader, refund.getCreatedBy());
            }
            return null;
        });
    }
    void assertNotCanceled() {
        tx(() -> {
            assertEquals(ReservationStatus.CONFIRMED, em.find(Reservation.class, booked.id()).getReservationStatus());
            assertEquals(MeetingStatus.CONFIRMED, em.find(Meeting.class, meetingId).getStatus());
            assertEquals(0, returns());
            for (var p : booked.participants()) {
                assertEquals(100 - p.amount(), em.find(Wallet.class, wallets.get(p.memberId())).getBalance());
            }
            return null;
        });
    }

    @Test void normalCancelRefundsExactlyAndPreservesReadHistory() {
        book(); var result = cancel();
        assertEquals(ReservationStatus.CANCELED, result.reservationStatus());
        assertEquals(new HashSet<>(booked.participants()), new HashSet<>(result.participants()));
        assertCanceled();
        assertEquals(ReservationStatus.CANCELED, service.getReservation(studyId, booked.id(), people.get(1)).reservationStatus());
        assertEquals(ReservationStatus.CANCELED, service.getReservations(studyId, leader, 0, 10).content().getFirst().reservationStatus());
    }
    @Test void repeatedCancelAfterDeadlineDoesNotRefundTwice() {
        book(); cancel();
        try (var clock = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(KOREA)).thenReturn(start.plusDays(1));
            assertEquals(booked.id(), cancel().id());
        }
        assertCanceled();
    }
    @Test void zeroAmountParticipantProducesNoReturnTransaction() {
        tx(() -> { em.find(Room.class, roomId).update(null, null, 1L, leader); return null; });
        book(); assertTrue(booked.participants().stream().anyMatch(p -> p.amount() == 0));
        cancel(); assertCanceled();
    }
    @Test void absentLeaderCanCancelAndHasNoRefund() {
        tx(() -> {
            em.createQuery("delete from MeetingRsvp r where r.meetingId=:id and r.memberId=:member")
                    .setParameter("id", meetingId).setParameter("member", leader).executeUpdate();
            attend(meetingId, people.get(3)); return null;
        });
        book(); cancel(); assertCanceled();
        assertTrue(booked.participants().stream().noneMatch(p -> p.memberId().equals(leader)));
    }
    @Test void deletedAndDepartedParticipantsStillReceiveRefund() {
        book(); memberService.deleteMember(people.get(1));
        tx(() -> {
            em.createQuery("select s from StudyMember s where s.studyId=:study and s.memberId=:member", StudyMember.class)
                    .setParameter("study", studyId).setParameter("member", people.get(2)).getSingleResult().leave(people.get(2));
            return null;
        });
        cancel(); assertCanceled();
    }
    @Test void onlyCurrentLeaderCanCancelAfterTransfer() {
        book(); UUID former = leader; leader = people.get(4);
        tx(() -> { em.find(Study.class, studyId).transferLeadership(leader, former); return null; });
        assertThrows(ForbiddenException.class, () -> service.cancel(studyId, booked.id(), former));
        cancel(); assertCanceled();
    }
    @Test void ordinaryMemberCannotCancel() {
        book(); assertThrows(ForbiddenException.class, () -> service.cancel(studyId, booked.id(), people.get(1)));
        assertNotCanceled();
    }
    @Test void deletedLeaderCannotCancel() {
        book(); memberService.deleteMember(leader);
        assertThrows(NotFoundException.class, this::cancel); assertNotCanceled();
    }
    @Test void suspendedLeaderCannotCancel() {
        book(); tx(() -> {
            org.springframework.test.util.ReflectionTestUtils.setField(em.find(Member.class, leader), "status", MemberStatus.SUSPENDED);
            return null;
        });
        assertThrows(ForbiddenException.class, this::cancel); assertNotCanceled();
    }
    @Test void wrongStudyCannotCancelReservation() {
        book(); UUID other = tx(this::newStudy);
        assertThrows(NotFoundException.class, () -> service.cancel(other, booked.id(), leader)); assertNotCanceled();
    }
    @Test void missingReservationAndNullInputsAreRejected() {
        book();
        assertThrows(NotFoundException.class, () -> service.cancel(studyId, UUID.randomUUID(), leader));
        assertThrows(IllegalArgumentException.class, () -> service.cancel(null, booked.id(), leader));
        assertThrows(IllegalArgumentException.class, () -> service.cancel(studyId, null, leader));
        assertThrows(IllegalArgumentException.class, () -> service.cancel(studyId, booked.id(), null));
        assertNotCanceled();
    }
    @Test void inactiveFacilitiesDoNotPreventCancellation() {
        book(); tx(() -> {
            em.find(Room.class, roomId).changeStatus(RoomStatus.INACTIVE, leader);
            em.find(Branch.class, branchId).changeStatus(BranchStatus.INACTIVE, leader); return null;
        });
        cancel(); assertCanceled();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {-1, 0, 1})
    void finalTwentyFourHourBoundary(long nanos) {
        book();
        try (var clock = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(KOREA)).thenReturn(start.minusHours(24).plusNanos(nanos));
            if (nanos <= 0) cancel(); else assertThrows(ConflictException.class, this::cancel);
        }
        if (nanos <= 0) assertCanceled(); else assertNotCanceled();
    }
    @Test void crossingDeadlineDuringFlushRollsBackAllRefundsAndStates() {
        book(); var flushed = new java.util.concurrent.atomic.AtomicBoolean();
        Mockito.doAnswer(call -> { Object result = call.callRealMethod(); flushed.set(true); return result; })
                .when(reservationRepository).flush();
        try (var clock = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(KOREA)).thenAnswer(call ->
                    flushed.get() ? start.minusHours(24).plusNanos(1) : start.minusHours(24).minusSeconds(1));
            assertThrows(ConflictException.class, this::cancel);
        }
        assertTrue(flushed.get()); assertNotCanceled();
    }
    @Test void missingLastPayersDebitRollsBackEarlierRefunds() {
        book(); UUID lastWallet = wallets.get(people.get(2));
        tx(() -> { em.createQuery("delete from WalletTransaction t where t.walletId=:wallet")
                .setParameter("wallet", lastWallet).executeUpdate(); return null; });
        assertThrows(ConflictException.class, this::cancel); assertNotCanceled();
    }
    @Test void databaseFailureAtFinalFlushRollsBackEverything() {
        book(); tx(() -> {
            em.createNativeQuery("alter table reservation add constraint test_no_cancel check (reservation_status <> 'CANCELED')").executeUpdate();
            return null;
        });
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, this::cancel);
            assertNotCanceled();
        } finally {
            tx(() -> { em.createNativeQuery("alter table reservation drop constraint test_no_cancel").executeUpdate(); return null; });
        }
    }
    @Test void futureReservationBlocksStudyEndAndDelete() {
        book(); assertThrows(ConflictException.class, this::endStudy);
        assertThrows(ConflictException.class, () -> studyService.delete(studyId, leader));
        tx(() -> {
            assertEquals(StudyStatus.RECRUITING, em.find(Study.class, studyId).getStatus());
            assertNull(em.find(Study.class, studyId).getDeletedAt()); return null;
        });
        assertNotCanceled();
    }
    @Test void cancelAllowsStudyEndAndDelete() {
        book(); cancel(); endStudy(); studyService.delete(studyId, leader);
        tx(() -> {
            assertEquals(StudyStatus.ENDED, em.find(Study.class, studyId).getStatus());
            assertNotNull(em.find(Study.class, studyId).getDeletedAt()); return null;
        }); assertCanceled();
    }
    @Test void reservationQueryFiltersStudyStatusAndStartBoundary() {
        book(); UUID empty = tx(this::newStudy);
        // PostgreSQL timestamps/JDBC parameters have microsecond precision.
        assertTrue(reservationRepository.existsFutureConfirmedReservation(studyId, start.minusNanos(1_000)));
        assertFalse(reservationRepository.existsFutureConfirmedReservation(studyId, start));
        assertFalse(reservationRepository.existsFutureConfirmedReservation(empty, start.minusNanos(1_000)));
        cancel(); assertFalse(reservationRepository.existsFutureConfirmedReservation(studyId, start.minusNanos(1_000)));
    }
    @Test void startedReservationDoesNotBlockStudyEndAndDelete() {
        book();
        try (var clock = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(KOREA)).thenReturn(start);
            endStudy(); studyService.delete(studyId, leader);
        }
        assertNotCanceled();
    }
    @Test void ordinaryMeetingCancelCannotBypassRefund() {
        book(); assertThrows(ConflictException.class, () -> meetingService.cancel(studyId, meetingId, leader));
        assertNotCanceled();
    }
    @Test void proposedMeetingCanStillBeCanceledNormally() {
        meetingService.cancel(studyId, meetingId, leader);
        tx(() -> { assertEquals(MeetingStatus.CANCELED, em.find(Meeting.class, meetingId).getStatus()); assertEquals(0, returns()); return null; });
    }
    @Test void canceledRoomSlotCanBeBookedByAnotherStudy() {
        book(); cancel();
        UUID nextMeeting = tx(() -> {
            UUID nextStudy = newStudy(); UUID next = newMeeting(nextStudy, start, start.plusHours(2), 100);
            for (int i = 0; i < 3; i++) attend(next, people.get(i)); return next;
        });
        assertNotEquals(booked.id(), reserve(nextMeeting, roomId).id());
    }

    void awaitLockWait() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < deadline) {
            em.createNativeQuery("select pg_stat_clear_snapshot()").getSingleResult();
            Number waiting = (Number) em.createNativeQuery("select count(*) from pg_stat_activity where datname=current_database() and application_name='reservation-cancel-test' and pid<>pg_backend_pid() and wait_event_type='Lock'").getSingleResult();
            if (waiting.longValue() > 0) return;
            try { Thread.sleep(25); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        }
        fail("Worker did not reach PostgreSQL lock wait");
    }
    @Test void concurrentDuplicateCancelWaitsAndRefundsOnce() throws Exception {
        book(); ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<ReservationResponse> worker = tx(() -> {
                cancel();
                Future<ReservationResponse> future = pool.submit(this::cancel);
                awaitLockWait(); assertFalse(future.isDone()); return future;
            });
            assertEquals(booked.id(), worker.get(20, TimeUnit.SECONDS).id()); assertCanceled();
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS)); }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void leaderDeletedWhileCancelWaitsOnRoomIsRecheckedAndRefundsRollback(boolean absent) throws Exception {
        if (absent) {
            tx(() -> {
                em.createQuery("delete from MeetingRsvp r where r.meetingId=:id and r.memberId=:member")
                        .setParameter("id", meetingId).setParameter("member", leader).executeUpdate();
                attend(meetingId, people.get(3)); return null;
            });
        }
        book(); ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Object> worker = tx(() -> {
                rooms.findByIdForUpdate(roomId).orElseThrow();
                Future<Object> future = pool.submit(() -> { try { return cancel(); } catch (RuntimeException e) { return e; } });
                awaitLockWait(); memberService.deleteMember(leader); return future;
            });
            assertInstanceOf(NotFoundException.class, worker.get(20, TimeUnit.SECONDS)); assertNotCanceled();
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS)); }
    }
    @Test void studyEndWaitsForCancellationThenSucceeds() throws Exception {
        book(); ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> worker = tx(() -> {
                cancel(); Future<?> future = pool.submit(this::endStudy);
                awaitLockWait(); assertFalse(future.isDone()); return future;
            });
            worker.get(20, TimeUnit.SECONDS); assertCanceled();
            tx(() -> { assertEquals(StudyStatus.ENDED, em.find(Study.class, studyId).getStatus()); return null; });
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS)); }
    }
    org.springframework.test.web.servlet.MockMvc mvc() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
    }
    org.springframework.test.web.servlet.request.RequestPostProcessor as(UUID id) {
        var principal = new com.studymate.auth.infrastructure.MemberPrincipal(id, "tester@example.com", MemberRole.USER);
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    String url() { return "/api/studies/" + studyId + "/reservations/" + booked.id() + "/cancel"; }
    @Test void httpCancelReturns200AndCanceledResponse() throws Exception {
        book(); mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url()).with(as(leader)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.reservationStatus").value("CANCELED"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.participants.length()").value(3));
        assertCanceled();
    }
    @Test void httpAnonymousAndNonLeaderAreRejected() throws Exception {
        book(); var mvc = mvc();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url()).with(as(people.get(1))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        assertNotCanceled();
    }
    @Test void httpLateCancelReturns409AndRollsBack() throws Exception {
        book();
        try (var clock = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(KOREA)).thenReturn(start.minusHours(23));
            mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url()).with(as(leader)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
        }
        assertNotCanceled();
    }
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    @Autowired ReservationService service;
    @Autowired MemberService memberService;
    @Autowired MemberRepository members;
    @Autowired BranchRepository branches;
    @Autowired RoomRepository rooms;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    ReservationRepository reservationRepository;
    UUID leader, studyId, meetingId, branchId, roomId;
    List<UUID> people;
    Map<UUID, UUID> wallets;
    LocalDateTime start;

    <T> T tx(Supplier<T> action) {
        return new TransactionTemplate(manager).execute(status -> action.get());
    }

    @BeforeEach void fixture() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith(
                    "jdbc:postgresql://127.0.0.1:55439/reservation_cancel_test"));
        }
        people = new ArrayList<>();
        wallets = new HashMap<>();
        start = LocalDate.now(KOREA).plusDays(4).atTime(10, 0);
        tx(() -> {
            for (int i = 0; i < 5; i++) {
                Member member = new Member(UUID.randomUUID() + "@example.com", "test-password", "tester", ActivityRegion.SEOUL_GANGNAM);
                em.persist(member);
                people.add(member.getId());
                Wallet wallet = new Wallet(member.getId(), member.getId());
                // Fixture funding only; transaction history assertions cover reservation debits.
                wallet.credit(100, member.getId());
                em.persist(wallet);
                wallets.put(member.getId(), wallet.getId());
            }
            people.sort(UUID::compareTo);
            leader = people.get(0);
            Branch branch = new Branch("test branch", "test address", leader);
            em.persist(branch); branchId = branch.getId();
            Room room = new Room(branchId, "test room", 5, 50, leader);
            em.persist(room); roomId = room.getId();
            studyId = newStudy();
            meetingId = newMeeting(studyId, start, start.plusHours(2), 100);
            for (int i = 0; i < 3; i++) attend(meetingId, people.get(i));
            return null;
        });
    }

    UUID newStudy() {
        Study study = new Study("test study", "description", leader, 10, ActivityRegion.SEOUL_GANGNAM);
        em.persist(study);
        for (UUID person : people) {
            StudyMember membership = new StudyMember(study.getId(), person, null);
            membership.approve(leader);
            em.persist(membership);
        }
        return study.getId();
    }
    UUID newMeeting(UUID study, LocalDateTime from, LocalDateTime to, long max) {
        Meeting meeting = new Meeting(study, branchId, "test meeting", "description", max, from, to, leader);
        em.persist(meeting);
        return meeting.getId();
    }
    void attend(UUID meeting, UUID person) {
        em.persist(new MeetingRsvp(meeting, person, MeetingRsvpStatus.ATTENDING, null, person));
    }
    ReservationResponse reserve() { return reserve(meetingId, roomId); }
    ReservationResponse reserve(UUID meeting, UUID room) {
        return service.create(leader, new ReservationCreateRequest(meeting, room));
    }
    long count(String entity) {
        return em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult();
    }
    @AfterEach void clean() {
        // This context is hard-wired to reservation_cancel_test on 55439, never the development DB.
        tx(() -> {
            for (String entity : List.of("WalletTransaction", "ReservationParticipant", "Reservation", "MeetingRsvp", "Meeting", "StudyMember", "Study", "Room", "Branch", "Wallet", "Member")) {
                em.createQuery("delete from " + entity).executeUpdate();
            }
            return null;
        });
    }

}

