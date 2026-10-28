package com.studymate.reservation;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.global.domain.ActivityRegion;
import com.studymate.global.exception.*;
import com.studymate.meeting.domain.Meeting;
import com.studymate.member.domain.*;
import com.studymate.reservation.application.service.ReservationService;
import com.studymate.reservation.domain.branch.*;
import com.studymate.reservation.domain.room.*;
import com.studymate.reservation.domain.reservation.*;
import com.studymate.study.domain.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Explicit, opt-in disposable database only; each test rolls back its own fixtures. */
@EnabledIfEnvironmentVariable(named = "RESERVATION_PG_TEST", matches = "true")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55438/reservation_read_test?ApplicationName=reservation-read-test",
    "spring.datasource.username=postgres", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=reservation-read-test-only-secret-012345678901234567890123456789",
    "jwt.access-token-expiration-seconds=3600"
})
@Transactional
class ReservationReadPostgresIntegrationTest {
    @Autowired EntityManager em;
    @Autowired ReservationService service;
    @Autowired WebApplicationContext context;
    @Autowired DataSource dataSource;
    MockMvc mvc;
    UUID leader, viewer, outsider, studyId, otherStudyId, membershipId;
    UUID reservationId, otherReservationId, roomId, branchId;
    final LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);

    @BeforeEach void fixture() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith(
                    "jdbc:postgresql://127.0.0.1:55438/reservation_read_test"));
        }
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        leader = member(); viewer = member(); outsider = member();
        Study study = new Study("read study", "description", leader, 10, ActivityRegion.SEOUL_GANGNAM);
        em.persist(study); studyId = study.getId();
        Study other = new Study("other study", "description", leader, 10, ActivityRegion.SEOUL_GANGNAM);
        em.persist(other); otherStudyId = other.getId();
        StudyMember membership = new StudyMember(studyId, viewer, null);
        membership.approve(leader); em.persist(membership); membershipId = membership.getId();
        Branch branch = new Branch("branch", "address", leader);
        em.persist(branch); branchId = branch.getId();
        Room room = new Room(branchId, "room", 10, 1, leader);
        em.persist(room); roomId = room.getId();
        reservationId = reservation(studyId, start);
        otherReservationId = reservation(otherStudyId, start.plusDays(5));
        // The reader is deliberately neither the leader nor a reservation participant.
        em.persist(new ReservationParticipant(reservationId, leader, 1, leader));
        em.persist(new ReservationParticipant(reservationId, outsider, 0, leader));
        flushAndClear();
    }

    UUID member() {
        Member member = new Member(UUID.randomUUID() + "@example.com", "encoded-password", "reader", ActivityRegion.SEOUL_GANGNAM);
        em.persist(member); return member.getId();
    }
    UUID reservation(UUID study, LocalDateTime at) {
        Meeting meeting = new Meeting(study, branchId, "meeting", "description", 10, at, at.plusHours(1), leader);
        em.persist(meeting);
        Reservation reservation = new Reservation(meeting.getId(), roomId, at, at.plusHours(1), 1, leader);
        em.persist(reservation); meeting.confirm(leader);
        return reservation.getId();
    }
    void flushAndClear() { em.flush(); em.clear(); }
    RequestPostProcessor as(UUID memberId) {
        MemberPrincipal principal = new MemberPrincipal(memberId, "reader@example.com", MemberRole.USER);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    String listUrl() { return "/api/studies/" + studyId + "/reservations"; }
    String detailUrl() { return listUrl() + "/" + reservationId; }
    void assertBothDenied(Class<? extends Throwable> type, UUID reader) {
        assertThrows(type, () -> service.getReservation(studyId, reservationId, reader));
        assertThrows(type, () -> service.getReservations(studyId, reader, 0, 10));
    }

    @Test void activeNonParticipantCanReadStoredDetailIncludingZeroAmount() {
        var response = service.getReservation(studyId, reservationId, viewer);
        assertEquals(reservationId, response.id());
        assertEquals(roomId, response.roomId());
        assertEquals(start, response.startAt());
        assertEquals(start.plusHours(1), response.endAt());
        assertEquals(1, response.hourlyRate());
        assertEquals(1, response.totalAmount());
        assertEquals(ReservationStatus.CONFIRMED, response.reservationStatus());
        assertEquals(2, response.participants().size());
        assertTrue(response.participants().stream().anyMatch(p -> p.memberId().equals(outsider) && p.amount() == 0));
        assertTrue(response.participants().stream().noneMatch(p -> p.memberId().equals(viewer)));
    }
    @Test void canceledReservationAndInactiveFacilitiesRemainReadable() {
        em.find(Reservation.class, reservationId).cancel(leader);
        em.find(Branch.class, branchId).changeStatus(BranchStatus.INACTIVE, leader);
        em.find(Room.class, roomId).changeStatus(RoomStatus.INACTIVE, leader);
        flushAndClear();
        assertEquals(ReservationStatus.CANCELED, service.getReservation(studyId, reservationId, viewer).reservationStatus());
        var page = service.getReservations(studyId, viewer, 0, 10);
        assertEquals(1, page.totalElements());
        assertEquals(ReservationStatus.CANCELED, page.content().getFirst().reservationStatus());
    }
    @Test void anotherStudysReservationIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getReservation(studyId, otherReservationId, viewer));
    }
    @Test void missingReservationIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getReservation(studyId, UUID.randomUUID(), viewer));
    }
    @Test void outsiderIsDeniedBothReads() { assertBothDenied(ForbiddenException.class, outsider); }
    @Test void missingMemberIsDeniedBothReads() { assertBothDenied(NotFoundException.class, UUID.randomUUID()); }
    @Test void deletedMemberIsDeniedBothReads() {
        em.find(Member.class, viewer).delete(viewer); flushAndClear();
        assertBothDenied(NotFoundException.class, viewer);
    }
    @Test void suspendedMemberIsDeniedBothReads() {
        ReflectionTestUtils.setField(em.find(Member.class, viewer), "status", MemberStatus.SUSPENDED);
        flushAndClear(); assertBothDenied(ForbiddenException.class, viewer);
    }
    @ParameterizedTest
    @EnumSource(value = StudyMemberStatus.class, names = {"PENDING_APPROVAL", "LEFT", "REJECTED", "CANCELED", "COMPLETE"})
    void nonActiveMembershipCannotReadOngoingStudy(StudyMemberStatus status) {
        ReflectionTestUtils.setField(em.find(StudyMember.class, membershipId), "status", status);
        flushAndClear(); assertBothDenied(ForbiddenException.class, viewer);
    }
    @Test void completedMemberCanReadEndedStudy() {
        em.find(Study.class, studyId).changeStatus(StudyStatus.ENDED, leader);
        em.find(StudyMember.class, membershipId).complete(leader);
        flushAndClear();
        assertEquals(reservationId, service.getReservation(studyId, reservationId, viewer).id());
        assertEquals(1, service.getReservations(studyId, viewer, 0, 10).totalElements());
    }
    @Test void latestPendingMembershipOverridesOldActiveRecord() {
        StudyMember pending = new StudyMember(studyId, viewer, null);
        ReflectionTestUtils.setField(pending, "createdAt", LocalDateTime.now().plusDays(1));
        em.persist(pending); flushAndClear();
        assertBothDenied(ForbiddenException.class, viewer);
    }
    @Test void deletedStudyIsNotFoundForBothReads() {
        em.find(Study.class, studyId).delete(leader); flushAndClear();
        assertBothDenied(NotFoundException.class, viewer);
    }
    @Test void missingStudyIsNotFoundForBothReads() {
        studyId = UUID.randomUUID(); assertBothDenied(NotFoundException.class, viewer);
    }
    @Test void pagingFiltersByStudyAndSortsByStartThenPostgresUuidDescending() {
        UUID newerA = reservation(studyId, start.plusDays(1));
        UUID newerB = reservation(studyId, start.plusDays(1));
        flushAndClear();
        // PostgreSQL UUID ordering is unsigned lexical, unlike Java UUID.compareTo.
        List<UUID> tied = new ArrayList<>(List.of(newerA, newerB));
        tied.sort(Comparator.comparing(UUID::toString).reversed());
        var first = service.getReservations(studyId, viewer, 0, 2);
        assertEquals(tied, first.content().stream().map(r -> r.id()).toList());
        assertEquals(0, first.page()); assertEquals(2, first.size());
        assertEquals(3, first.totalElements()); assertEquals(2, first.totalPages());
        var second = service.getReservations(studyId, viewer, 1, 2);
        assertEquals(List.of(reservationId), second.content().stream().map(r -> r.id()).toList());
        assertEquals(3, second.totalElements()); assertEquals(2, second.totalPages());
    }
    @Test void emptyStudyReturnsEmptyPage() {
        Study empty = new Study("empty", "description", viewer, 10, ActivityRegion.SEOUL_GANGNAM);
        em.persist(empty); em.persist(StudyMember.createLeader(empty.getId(), viewer));
        UUID emptyId = empty.getId(); flushAndClear();
        var response = service.getReservations(emptyId, viewer, 0, 10);
        assertTrue(response.content().isEmpty()); assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
    }
    @Test void beyondLastPageIsEmptyButRetainsTotalCount() {
        var page = service.getReservations(studyId, viewer, 5, 10);
        assertTrue(page.content().isEmpty()); assertEquals(1, page.totalElements()); assertEquals(5, page.page());
    }
    @ParameterizedTest @CsvSource({"-1,10", "0,0", "0,101"})
    void invalidPaginationIsRejected(int page, int size) {
        assertThrows(IllegalArgumentException.class, () -> service.getReservations(studyId, viewer, page, size));
    }
    @Test void nullIdentifiersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.getReservation(null, reservationId, viewer));
        assertThrows(IllegalArgumentException.class, () -> service.getReservation(studyId, null, viewer));
        assertThrows(IllegalArgumentException.class, () -> service.getReservation(studyId, reservationId, null));
        assertThrows(IllegalArgumentException.class, () -> service.getReservations(null, viewer, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> service.getReservations(studyId, null, 0, 10));
    }
    @Test void httpDetailReturnsParticipantData() throws Exception {
        mvc.perform(get(detailUrl()).with(as(viewer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(reservationId.toString()))
                .andExpect(jsonPath("$.hourlyRate").value(1))
                .andExpect(jsonPath("$.participants.length()").value(2));
    }
    @Test void httpListUsesPluralPathDefaultsAndSummaryShape() throws Exception {
        mvc.perform(get(listUrl()).with(as(viewer))).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(reservationId.toString()))
                .andExpect(jsonPath("$.content[0].participants").doesNotExist())
                .andExpect(jsonPath("$.content[0].hourlyRate").doesNotExist());
    }
    @Test void httpExplicitPagingReachesService() throws Exception {
        reservation(studyId, start.plusDays(1)); flushAndClear();
        mvc.perform(get(listUrl()).param("page", "1").param("size", "1").with(as(viewer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1)).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(reservationId.toString()));
    }
    @Test void httpAnonymousReadsRequireAuthentication() throws Exception {
        mvc.perform(get(listUrl())).andExpect(status().isUnauthorized());
        mvc.perform(get(detailUrl())).andExpect(status().isUnauthorized());
    }
    @Test void httpOutsiderReadsAreForbidden() throws Exception {
        mvc.perform(get(listUrl()).with(as(outsider))).andExpect(status().isForbidden());
        mvc.perform(get(detailUrl()).with(as(outsider))).andExpect(status().isForbidden());
    }
    @Test void httpWrongStudyReservationReturns404() throws Exception {
        mvc.perform(get(listUrl() + "/" + otherReservationId).with(as(viewer))).andExpect(status().isNotFound());
    }
    @ParameterizedTest @CsvSource({"page,-1", "size,0", "size,101", "page,abc"})
    void httpInvalidPaginationReturns400(String key, String value) throws Exception {
        mvc.perform(get(listUrl()).param(key, value).with(as(viewer))).andExpect(status().isBadRequest());
    }
    @Test void httpInvalidReservationUuidReturns400() throws Exception {
        mvc.perform(get(listUrl() + "/invalid").with(as(viewer))).andExpect(status().isBadRequest());
    }
}
