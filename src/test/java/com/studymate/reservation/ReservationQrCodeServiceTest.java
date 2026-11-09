package com.studymate.reservation;

import com.studymate.reservation.application.service.ReservationQrCodeService;
import com.studymate.reservation.application.service.ReservationAttendanceService;
import com.studymate.reservation.domain.reservation.*;
import com.studymate.study.domain.*;
import com.studymate.member.domain.*;
import com.studymate.meeting.domain.*;
import com.studymate.global.exception.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ReservationQrCodeServiceTest {
    StudyRepository studies = mock(StudyRepository.class);
    MemberRepository members = mock(MemberRepository.class);
    ReservationRepository reservations = mock(ReservationRepository.class);
    MeetingRepository meetings = mock(MeetingRepository.class);
    ReservationParticipantRepository participants = mock(ReservationParticipantRepository.class);
    ReservationAttendanceRepository attendance = mock(ReservationAttendanceRepository.class);
    ReservationQrManagementRepository managements = mock(ReservationQrManagementRepository.class);
    ReservationQrCodeRepository codes = mock(ReservationQrCodeRepository.class);
    ReservationAttendanceGrantRepository grants = mock(ReservationAttendanceGrantRepository.class);
    ReservationAttendanceService attendanceService = mock(ReservationAttendanceService.class);
    ReservationQrCodeService service = new ReservationQrCodeService(
            studies, members, reservations, meetings, participants, attendance, managements, codes,
            grants, attendanceService);
    UUID studyId = UUID.randomUUID(), reservationId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID(), participantId = UUID.randomUUID();
    ReservationQrManagement management = mock(ReservationQrManagement.class);
    LocalDateTime start = LocalDateTime.of(2030, 1, 10, 13, 0);
    ZoneId korea = ZoneId.of("Asia/Seoul");

    @BeforeEach void setup() {
        when(studies.findByIdForUpdate(studyId)).thenReturn(Optional.of(mock(Study.class)));
        Member member = mock(Member.class);
        when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
        when(members.findByIdAndDeletedAtIsNull(memberId)).thenReturn(Optional.of(member));
        Reservation reservation = mock(Reservation.class);
        UUID meetingId = UUID.randomUUID();
        when(reservation.getMeetingId()).thenReturn(meetingId);
        when(reservation.getReservationStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getStartAt()).thenReturn(start);
        when(reservation.getEndAt()).thenReturn(start.plusHours(3));
        when(reservations.findById(reservationId)).thenReturn(Optional.of(reservation));
        Meeting meeting = mock(Meeting.class);
        when(meeting.getStudyId()).thenReturn(studyId);
        when(meetings.findById(meetingId)).thenReturn(Optional.of(meeting));
        ReservationParticipant participant = mock(ReservationParticipant.class);
        when(participant.getMemberId()).thenReturn(memberId);
        when(participant.getId()).thenReturn(participantId);
        when(participants.findAllByReservationId(reservationId)).thenReturn(List.of(participant));
        when(managements.findByReservationId(reservationId)).thenReturn(Optional.of(management));
        when(management.getOperatorParticipantId()).thenReturn(participantId);
    }

    @Test void storesHashAndReturnsDistinctTokensWithSixtySecondLifetime() throws Exception {
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start);
            var first = service.issue(studyId, reservationId, memberId);
            var second = service.issue(studyId, reservationId, memberId);
            assertNotEquals(first.token(), second.token());
            assertEquals(32, Base64.getUrlDecoder().decode(first.token()).length);
            assertEquals(start.plusSeconds(60), first.expiresAt());
            var saved = ArgumentCaptor.forClass(ReservationQrCode.class);
            verify(codes, times(2)).save(saved.capture());
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(first.token().getBytes(StandardCharsets.UTF_8)));
            assertEquals(hash, saved.getAllValues().getFirst().getTokenHash());
            assertEquals(reservationId, saved.getValue().getReservationId());
            assertEquals(participantId, saved.getValue().getIssuedByParticipantId());
        }
    }

    @Test void previousOperatorCannotIssueAfterHandover() {
        when(management.getOperatorParticipantId()).thenReturn(UUID.randomUUID());
        assertThrows(ForbiddenException.class, () -> service.issue(studyId, reservationId, memberId));
        verifyNoInteractions(codes);
    }

    @Test void checkedOutOperatorCannotIssue() {
        ReservationAttendance record = new ReservationAttendance(participantId, start);
        record.checkOut(start.plusHours(1));
        when(attendance.findByReservationParticipantId(participantId)).thenReturn(Optional.of(record));
        assertThrows(ConflictException.class, () -> service.issue(studyId, reservationId, memberId));
        verifyNoInteractions(codes);
    }

    @ParameterizedTest
    @CsvSource({"12:49:59.999999,false", "12:50:00,true", "16:10:59.999999,true", "16:11:00,false"})
    void issuanceWindow(LocalTime time, boolean allowed) {
        LocalDateTime now = start.toLocalDate().atTime(time);
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(now);
            if (allowed) {
                assertEquals(now, service.issue(studyId, reservationId, memberId).issuedAt());
                verify(codes).save(any());
            } else {
                assertThrows(ConflictException.class, () -> service.issue(studyId, reservationId, memberId));
                verifyNoInteractions(codes);
            }
        }
    }

    String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    ReservationQrCode sharedCode() throws Exception {
        ReservationQrCode code = new ReservationQrCode(reservationId, hash("shared"), participantId);
        when(codes.findByTokenHash(hash("shared"))).thenReturn(Optional.of(code));
        return code;
    }

    ReservationAttendanceGrant personalGrant(AttendanceAction action) throws Exception {
        var grant = new ReservationAttendanceGrant(participantId, hash("personal"), action);
        when(grants.findByTokenHash(hash("personal"))).thenReturn(Optional.of(grant));
        return grant;
    }

    @Test void scanFixesActionAndDoesNotRecordAttendance() throws Exception {
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start);
            sharedCode();
            var first = service.scan(studyId, reservationId, memberId, "shared");
            assertEquals(AttendanceAction.CHECK_IN, first.action());
            var in = new ReservationAttendance(participantId, start);
            when(attendance.findByReservationParticipantId(participantId)).thenReturn(Optional.of(in));
            var second = service.scan(studyId, reservationId, memberId, "shared");
            assertEquals(AttendanceAction.CHECK_OUT, second.action());
            assertNotEquals(first.token(), second.token());
            var captured = ArgumentCaptor.forClass(ReservationAttendanceGrant.class);
            verify(grants, times(2)).save(captured.capture());
            assertEquals(hash(first.token()), captured.getAllValues().getFirst().getTokenHash());
            assertNull(captured.getValue().getUsedAt());
            in.checkOut(start);
            var done = service.scan(studyId, reservationId, memberId, "shared");
            assertNull(done.token());
            assertNull(done.action());
            assertEquals(start, done.checkedOutAt());
            verify(grants, times(2)).save(any());
            verifyNoInteractions(attendanceService);
        }
    }

    @Test void grantSurvivesOriginalQrExpiryAndCannotBeReused() throws Exception {
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start);
            sharedCode();
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start.plusSeconds(59));
            var response = service.scan(studyId, reservationId, memberId, "shared");
            var captured = ArgumentCaptor.forClass(ReservationAttendanceGrant.class);
            verify(grants).save(captured.capture());
            var grant = captured.getValue();
            when(grants.findByTokenHash(hash(response.token()))).thenReturn(Optional.of(grant));
            assertEquals(start.plusSeconds(119), response.expiresAt());
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start.plusSeconds(61));
            service.checkInWithGrant(studyId, reservationId, memberId, response.token());
            assertEquals(start.plusSeconds(61), grant.getUsedAt());
            assertThrows(ConflictException.class, () ->
                    service.checkInWithGrant(studyId, reservationId, memberId, response.token()));
            verify(attendanceService, times(1)).checkIn(studyId, reservationId, memberId);
        }
    }

    @ParameterizedTest
    @CsvSource({"59,true", "60,false", "61,false"})
    void sharedQrExpiry(long seconds, boolean allowed) throws Exception {
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start);
            sharedCode();
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start.plusSeconds(seconds));
            if (allowed) assertNotNull(service.scan(studyId, reservationId, memberId, "shared").token());
            else assertThrows(ConflictException.class, () -> service.scan(studyId, reservationId, memberId, "shared"));
        }
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "59,true", "60,false", "61,false"})
    void personalGrantExpiry(long seconds, boolean allowed) throws Exception {
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start);
            var grant = personalGrant(AttendanceAction.CHECK_OUT);
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start.plusSeconds(seconds));
            if (allowed) {
                service.checkOutWithGrant(studyId, reservationId, memberId, "personal");
                assertEquals(start.plusSeconds(seconds), grant.getUsedAt());
                verify(attendanceService).checkOut(studyId, reservationId, memberId);
            } else {
                assertThrows(ConflictException.class, () -> service.checkOutWithGrant(studyId, reservationId, memberId, "personal"));
                assertNull(grant.getUsedAt());
                verifyNoInteractions(attendanceService);
            }
        }
    }

    @Test void rejectsWrongOwnerWrongActionAndMissingToken() throws Exception {
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start);
            var grant = personalGrant(AttendanceAction.CHECK_IN);
            assertThrows(ForbiddenException.class, () -> service.checkOutWithGrant(studyId, reservationId, memberId, "personal"));
            assertThrows(IllegalArgumentException.class, () -> service.checkInWithGrant(studyId, reservationId, memberId, ""));
            var otherGrant = new ReservationAttendanceGrant(UUID.randomUUID(), hash("personal"), AttendanceAction.CHECK_IN);
            when(grants.findByTokenHash(hash("personal"))).thenReturn(Optional.of(otherGrant));
            assertThrows(ForbiddenException.class, () -> service.checkInWithGrant(studyId, reservationId, memberId, "personal"));
            assertNull(grant.getUsedAt());
            verifyNoInteractions(attendanceService);
        }
    }

    @Test void failedAttendanceDoesNotConsumeGrant() throws Exception {
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(start);
            var grant = personalGrant(AttendanceAction.CHECK_IN);
            when(attendanceService.checkIn(studyId, reservationId, memberId))
                    .thenThrow(new ConflictException("입실 마감"));
            assertThrows(ConflictException.class, () -> service.checkInWithGrant(studyId, reservationId, memberId, "personal"));
            assertNull(grant.getUsedAt());
        }
    }

    @Test void onlyCurrentOperatorCanDirectlyCheckInAndOut() {
        service.checkInAsOperator(studyId, reservationId, memberId);
        service.checkOutAsOperator(studyId, reservationId, memberId);
        verify(attendanceService).checkIn(studyId, reservationId, memberId);
        verify(attendanceService).checkOut(studyId, reservationId, memberId);
        when(management.getOperatorParticipantId()).thenReturn(UUID.randomUUID());
        assertThrows(ForbiddenException.class, () -> service.checkInAsOperator(studyId, reservationId, memberId));
        assertThrows(ForbiddenException.class, () -> service.checkOutAsOperator(studyId, reservationId, memberId));
        verifyNoMoreInteractions(attendanceService);
        verifyNoInteractions(grants);
    }

    @ParameterizedTest
    @CsvSource({"CHECK_IN,12:49:59.999999,false", "CHECK_IN,12:50:00,true",
            "CHECK_IN,15:49:59.999999,true", "CHECK_IN,15:50:00,false",
            "CHECK_OUT,16:10:59.999999,true", "CHECK_OUT,16:11:00,false"})
    void scanRespectsAttendanceWindow(AttendanceAction action, LocalTime time, boolean allowed) throws Exception {
        LocalDateTime at = start.toLocalDate().atTime(time);
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(() -> LocalDateTime.now(korea)).thenReturn(at);
            sharedCode();
            if (action == AttendanceAction.CHECK_OUT) {
                when(attendance.findByReservationParticipantId(participantId))
                        .thenReturn(Optional.of(new ReservationAttendance(participantId, start)));
            }
            if (allowed) assertEquals(action, service.scan(studyId, reservationId, memberId, "shared").action());
            else {
                assertThrows(ConflictException.class, () -> service.scan(studyId, reservationId, memberId, "shared"));
                verify(grants, never()).save(any());
            }
        }
    }
}
