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
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55437/reservation_test?ApplicationName=reservation-integration-test",
    "spring.datasource.username=postgres", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=reservation-test-only-secret-012345678901234567890123456789",
    "jwt.access-token-expiration-seconds=3600"
})
class ReservationPostgresIntegrationTest {
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

    @BeforeEach void fixture() {
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
    void unchanged() {
        tx(() -> {
            assertEquals(0L, count("Reservation"));
            assertEquals(0L, count("ReservationParticipant"));
            assertEquals(0L, count("WalletTransaction"));
            assertEquals(MeetingStatus.PROPOSED, em.find(Meeting.class, meetingId).getStatus());
            for (UUID wallet : wallets.values()) assertEquals(100L, em.find(Wallet.class, wallet).getBalance());
            return null;
        });
    }
    void assertStored(ReservationResponse result, int participantCount, long total, long debitCount) {
        tx(() -> {
            assertEquals(1L, count("Reservation"));
            assertEquals(participantCount, count("ReservationParticipant"));
            assertEquals(debitCount, count("WalletTransaction"));
            assertEquals(MeetingStatus.CONFIRMED, em.find(Meeting.class, result.meetingId()).getStatus());
            assertEquals(total, em.find(Reservation.class, result.id()).getTotalAmount());
            long sum = 0;
            for (var participant : result.participants()) {
                sum += participant.amount();
                assertEquals(100 - participant.amount(), em.find(Wallet.class, wallets.get(participant.memberId())).getBalance());
            }
            assertEquals(total, sum);
            for (WalletTransaction transaction : em.createQuery("select t from WalletTransaction t", WalletTransaction.class).getResultList()) {
                assertEquals(result.id(), transaction.getReferenceId());
                assertEquals(WalletTransactionType.RESERVATION_DEBIT, transaction.getWalletTransactionType());
            }
            return null;
        });
    }
    @AfterEach void clean() {
        // This context is hard-wired to reservation_test on 55437, never the development DB.
        tx(() -> {
            for (String entity : List.of("WalletTransaction", "ReservationParticipant", "Reservation", "MeetingRsvp", "Meeting", "StudyMember", "Study", "Room", "Branch", "Wallet", "Member")) {
                em.createQuery("delete from " + entity).executeUpdate();
            }
            return null;
        });
    }

    @Test void createsReservationWithBalancedSplitAndPersistedDebits() {
        var response = reserve();
        assertEquals(List.of(33L, 33L, 34L), response.participants().stream().map(p -> p.amount()).sorted().toList());
        assertStored(response, 3, 100, 3);
    }
    @Test void zeroShareIsStoredWithoutWalletTransaction() {
        tx(() -> { em.find(Room.class, roomId).update(null, null, 1L, leader); return null; });
        var response = reserve();
        assertEquals(List.of(0L, 1L, 1L), response.participants().stream().map(p -> p.amount()).sorted().toList());
        assertStored(response, 3, 2, 2);
    }
    @Test void successfulRetryReturnsSameAllocationAndDoesNotDebitAgain() {
        var first = reserve();
        var second = reserve();
        assertEquals(first.id(), second.id());
        assertEquals(new HashSet<>(first.participants()), new HashSet<>(second.participants()));
        assertStored(second, 3, 100, 3);
    }
    @Test void differentRoomOnRetryConflicts() {
        var first = reserve();
        assertThrows(ConflictException.class, () -> reserve(meetingId, UUID.randomUUID()));
        assertStored(first, 3, 100, 3);
    }
    @Test void nonLeaderRejected() {
        assertThrows(ForbiddenException.class, () -> service.create(people.get(1), new ReservationCreateRequest(meetingId, roomId)));
        unchanged();
    }
    @Test void exactlyHalfRejectedIncludingUnansweredMembersInDenominator() {
        tx(() -> {
            Member sixth = new Member(UUID.randomUUID()+"@example.com", "test-password", "sixth", ActivityRegion.SEOUL_GANGNAM);
            em.persist(sixth);
            StudyMember sm = new StudyMember(studyId, sixth.getId(), null); sm.approve(leader); em.persist(sm);
            return null;
        });
        assertThrows(ConflictException.class, this::reserve); unchanged();
    }
    @Test void capacityExceededRejected() {
        tx(() -> { em.find(Room.class, roomId).update(null, 2, null, leader); return null; });
        assertThrows(ConflictException.class, this::reserve); unchanged();
    }
    @Test void inactiveRoomRejected() {
        tx(() -> { em.find(Room.class, roomId).changeStatus(RoomStatus.INACTIVE, leader); return null; });
        assertThrows(ConflictException.class, this::reserve); unchanged();
    }
    @Test void inactiveBranchRejected() {
        tx(() -> { em.find(Branch.class, branchId).changeStatus(BranchStatus.INACTIVE, leader); return null; });
        assertThrows(ConflictException.class, this::reserve); unchanged();
    }
    @Test void roomFromDifferentBranchRejected() {
        UUID otherRoom = tx(() -> {
            Branch b = new Branch("other", "address", leader); em.persist(b);
            Room r = new Room(b.getId(), "other", 5, 50, leader); em.persist(r); return r.getId();
        });
        assertThrows(ConflictException.class, () -> reserve(meetingId, otherRoom)); unchanged();
    }
    @Test void maxShareExceededRejected() {
        tx(() -> { em.createQuery("update Meeting m set m.maxCostPerPerson=33 where m.id=:id").setParameter("id", meetingId).executeUpdate(); return null; });
        assertThrows(ConflictException.class, this::reserve); unchanged();
    }
    @Test void lastPayerInsufficientRollsBackEarlierDebits() {
        UUID last = people.get(2);
        tx(() -> { em.find(Wallet.class, wallets.get(last)).debit(100, leader); return null; });
        assertThrows(ConflictException.class, this::reserve);
        tx(() -> {
            assertEquals(0, count("Reservation")); assertEquals(0, count("WalletTransaction"));
            assertEquals(0, count("ReservationParticipant"));
            assertEquals(100, em.find(Wallet.class, wallets.get(people.get(0))).getBalance());
            assertEquals(100, em.find(Wallet.class, wallets.get(people.get(1))).getBalance());
            assertEquals(0, em.find(Wallet.class, wallets.get(last)).getBalance());
            assertEquals(MeetingStatus.PROPOSED, em.find(Meeting.class, meetingId).getStatus());
            return null;
        });
    }
    @Test void deletedPayerRejected() {
        memberService.deleteMember(people.get(2));
        assertThrows(NotFoundException.class, this::reserve); unchanged();
    }
    @Test void outerFailureRollsBackCompleteReservation() {
        assertThrows(IllegalStateException.class, () -> tx(() -> { reserve(); throw new IllegalStateException("forced"); }));
        unchanged();
    }
    ReservationResponse atTime(LocalDateTime checkedAt) {
        try (MockedStatic<LocalDateTime> time = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            time.when(() -> LocalDateTime.now(KOREA)).thenReturn(checkedAt);
            return reserve();
        }
    }
    @Test void exact24HoursAllowed() {
        assertStored(atTime(start.minusHours(24)), 3, 100, 3);
    }
    @Test void oneNanosecondPastDeadlineRollsBackAllWrites() {
        assertThrows(ConflictException.class, () -> atTime(start.minusHours(24).plusNanos(1)));
        unchanged();
    }
    @Test void oneNanosecondBeforeDeadlineAllowed() {
        assertStored(atTime(start.minusHours(24).minusNanos(1)), 3, 100, 3);
    }
    UUID anotherMeeting(LocalDateTime from, LocalDateTime to) {
        return tx(() -> {
            UUID id = newMeeting(newStudy(), from, to, 100);
            for (int i = 0; i < 3; i++) attend(id, people.get(i));
            return id;
        });
    }
    @Test void overlapRejected() {
        UUID other = anotherMeeting(start.plusHours(1), start.plusHours(3));
        var first = reserve();
        assertThrows(ConflictException.class, () -> reserve(other, roomId));
        assertStored(first, 3, 100, 3);
    }
    @Test void adjacentReservationAllowed() {
        UUID other = anotherMeeting(start.plusHours(2), start.plusHours(3));
        reserve(); reserve(other, roomId);
        tx(() -> { assertEquals(2, count("Reservation")); assertEquals(6, count("ReservationParticipant")); return null; });
    }
    List<Object> race(Supplier<ReservationResponse> a, Supplier<ReservationResponse> b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (var action : List.of(a, b)) futures.add(pool.submit(() -> {
                ready.countDown(); assertTrue(go.await(5, TimeUnit.SECONDS));
                try { return action.get(); } catch (ConflictException e) { return e; }
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
            List<Object> results = new ArrayList<>();
            for (var f : futures) results.add(f.get(20, TimeUnit.SECONDS));
            return results;
        } finally { go.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS)); }
    }
    @Test void concurrentSameMeetingIsChargedOnce() throws Exception {
        var results = race(this::reserve, this::reserve);
        var a = assertInstanceOf(ReservationResponse.class, results.get(0));
        var b = assertInstanceOf(ReservationResponse.class, results.get(1));
        assertEquals(a.id(), b.id()); assertStored(a, 3, 100, 3);
    }
    @Test void concurrentDifferentStudiesCannotDoubleBookRoom() throws Exception {
        UUID other = anotherMeeting(start, start.plusHours(2));
        var results = race(this::reserve, () -> reserve(other, roomId));
        assertEquals(1, results.stream().filter(ReservationResponse.class::isInstance).count());
        assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count());
        assertStored((ReservationResponse) results.stream().filter(ReservationResponse.class::isInstance).findFirst().orElseThrow(), 3, 100, 3);
    }

    // Observe an actual PostgreSQL lock wait instead of assuming concurrency after a sleep.
    void awaitDatabaseLockWait() {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < end) {
            Number waiting = (Number) em.createNativeQuery("""
                    select count(*) from pg_stat_activity
                    where datname = current_database()
                      and application_name = 'reservation-integration-test'
                      and pid <> pg_backend_pid()
                      and wait_event_type = 'Lock'
                    """).getSingleResult();
            if (waiting.longValue() > 0) return;
            try { Thread.sleep(25); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            em.createNativeQuery("select pg_stat_clear_snapshot()").getSingleResult();
        }
        fail("Reservation worker never reached the expected PostgreSQL lock wait");
    }

    // Main transaction holds a row, worker demonstrably waits, main changes data then commits.
    Object waitingReservation(Runnable acquire, Runnable change) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Object> future = tx(() -> {
                acquire.run();
                Future<Object> worker = pool.submit(() -> {
                    try { return reserve(); }
                    catch (ConflictException | NotFoundException | ForbiddenException e) { return e; }
                });
                awaitDatabaseLockWait();
                change.run();
                return worker;
            });
            return future.get(20, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS)); }
    }

    @Test void branchDeactivationCommittedDuringWaitIsObserved() throws Exception {
        Object result = waitingReservation(
                () -> branches.findByIdForUpdate(branchId).orElseThrow(),
                () -> em.find(Branch.class, branchId).changeStatus(BranchStatus.INACTIVE, leader));
        assertInstanceOf(ConflictException.class, result); unchanged();
    }
    @Test void roomDeactivationCommittedDuringWaitIsObserved() throws Exception {
        Object result = waitingReservation(
                () -> rooms.findByIdForUpdate(roomId).orElseThrow(),
                () -> em.find(Room.class, roomId).changeStatus(RoomStatus.INACTIVE, leader));
        assertInstanceOf(ConflictException.class, result); unchanged();
    }
    @Test void roomPriceChangeCommittedDuringWaitIsUsed() throws Exception {
        Object result = waitingReservation(
                () -> rooms.findByIdForUpdate(roomId).orElseThrow(),
                () -> em.find(Room.class, roomId).update(null, null, 60L, leader));
        assertStored(assertInstanceOf(ReservationResponse.class, result), 3, 120, 3);
    }
    @Test void roomCapacityChangeCommittedDuringWaitIsObserved() throws Exception {
        Object result = waitingReservation(
                () -> rooms.findByIdForUpdate(roomId).orElseThrow(),
                () -> em.find(Room.class, roomId).update(null, 2, null, leader));
        assertInstanceOf(ConflictException.class, result); unchanged();
    }
    @Test void payingLeaderDeletionAfterInitialReadIsObserved() throws Exception {
        Object result = waitingReservation(
                () -> branches.findByIdForUpdate(branchId).orElseThrow(),
                () -> memberService.deleteMember(leader));
        assertInstanceOf(NotFoundException.class, result); unchanged();
    }
    void leaderAbsent() {
        tx(() -> {
            em.createQuery("delete from MeetingRsvp r where r.meetingId=:meeting and r.memberId=:member")
                    .setParameter("meeting", meetingId).setParameter("member", leader).executeUpdate();
            attend(meetingId, people.get(3));
            return null;
        });
    }
    @Test void absentLeaderCanReserveButIsNotStoredAsParticipant() {
        leaderAbsent();
        var result = reserve();
        assertTrue(result.participants().stream().noneMatch(p -> p.memberId().equals(leader)));
        assertStored(result, 3, 100, 3);
    }
    @Test void absentLeaderDeletionAfterInitialReadIsObserved() throws Exception {
        leaderAbsent();
        Object result = waitingReservation(
                () -> branches.findByIdForUpdate(branchId).orElseThrow(),
                () -> memberService.deleteMember(leader));
        assertInstanceOf(NotFoundException.class, result); unchanged();
    }
    @Test void leaderDeletionWaitsUntilSuccessfulReservationCommits() throws Exception {
        leaderAbsent();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> deletion = tx(() -> {
                reserve(); // Member lock remains held by this outer transaction.
                Future<?> worker = pool.submit(() -> memberService.deleteMember(leader));
                awaitDatabaseLockWait();
                assertFalse(worker.isDone());
                return worker;
            });
            deletion.get(20, TimeUnit.SECONDS);
            tx(() -> {
                assertEquals(1, count("Reservation"));
                assertEquals(3, count("WalletTransaction"));
                assertNotNull(em.find(Member.class, leader).getDeletedAt());
                return null;
            });
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS)); }
    }
    @Test void lateParticipantInsertFailureRollsBackWalletsAndMeeting() {
        tx(() -> {
            em.createNativeQuery("alter table reservation_participant add constraint test_reject_participant check (amount < 0)").executeUpdate();
            return null;
        });
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, this::reserve);
            unchanged();
        } finally {
            tx(() -> { em.createNativeQuery("alter table reservation_participant drop constraint test_reject_participant").executeUpdate(); return null; });
        }
    }
    @Test void deadlineCrossedDuringProcessingRollsBack() {
        LocalDateTime before = start.minusHours(24).minusSeconds(1);
        LocalDateTime after = start.minusHours(24).plusNanos(1);
        java.util.concurrent.atomic.AtomicBoolean flushed = new java.util.concurrent.atomic.AtomicBoolean();
        Mockito.doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            flushed.set(true);
            return result;
        }).when(reservationRepository).flush();
        try (MockedStatic<LocalDateTime> time = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            // Keep real DB writes and move only the clock after the real final flush.
            time.when(() -> LocalDateTime.now(KOREA)).thenAnswer(invocation -> flushed.get() ? after : before);
            assertThrows(ConflictException.class, this::reserve);
            assertTrue(flushed.get());
        }
        unchanged();
    }
}
