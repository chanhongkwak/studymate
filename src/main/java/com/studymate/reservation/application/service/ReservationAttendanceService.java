package com.studymate.reservation.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.reservation.application.dto.response.ReservationAttendanceResponse;
import com.studymate.reservation.domain.reservation.AttendanceStatus;
import com.studymate.reservation.domain.reservation.Reservation;
import com.studymate.reservation.domain.reservation.ReservationAttendance;
import com.studymate.reservation.domain.reservation.ReservationAttendanceRepository;
import com.studymate.reservation.domain.reservation.ReservationParticipant;
import com.studymate.reservation.domain.reservation.ReservationParticipantRepository;
import com.studymate.reservation.domain.reservation.ReservationRepository;
import com.studymate.reservation.domain.reservation.ReservationStatus;
import com.studymate.study.domain.StudyRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationAttendanceService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final ReservationAttendanceRepository reservationAttendanceRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationParticipantRepository reservationParticipantRepository;
    private final StudyRepository studyRepository;
    private final MeetingRepository meetingRepository;

    @Transactional
    public ReservationAttendance checkIn(UUID studyId, UUID reservationId, UUID memberId) {
        if (studyId == null || reservationId == null || memberId == null) {
            throw new IllegalArgumentException("스터디, 예약, 회원 ID는 필수입니다.");
        }

        LocalDateTime checkedInAt = LocalDateTime.now(KOREA_ZONE)
                .truncatedTo(ChronoUnit.MICROS);

        studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));

        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException("예약에 연결된 모임을 찾을 수 없습니다."));

        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }

        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약만 입실할 수 있습니다.");
        }

        ReservationParticipant participant = reservationParticipantRepository
                .findAllByReservationId(reservationId).stream()
                .filter(found -> memberId.equals(found.getMemberId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("예약 참석자만 입실할 수 있습니다."));

        Optional<ReservationAttendance> existing = reservationAttendanceRepository
                .findByReservationParticipantId(participant.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalDateTime opensAt = reservation.getStartAt().minusMinutes(10);
        LocalDateTime closesAt = reservation.getEndAt().minusMinutes(10);
        if (checkedInAt.isBefore(opensAt) || !checkedInAt.isBefore(closesAt)) {
            throw new ConflictException("입실 가능한 시간이 아닙니다.");
        }

        return reservationAttendanceRepository.save(
                new ReservationAttendance(participant.getId(), checkedInAt)
        );
    }

    @Transactional
    public ReservationAttendance checkOut(
            UUID studyId,
            UUID reservationId,
            UUID memberId
    ) {
        if (studyId == null || reservationId == null || memberId == null) {
            throw new IllegalArgumentException("스터디, 예약, 회원 ID는 필수입니다.");
        }

        LocalDateTime checkedOutAt = LocalDateTime.now(KOREA_ZONE)
                .truncatedTo(ChronoUnit.MICROS);

        studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));

        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException("예약에 연결된 모임을 찾을 수 없습니다."));

        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }

        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약만 퇴실할 수 있습니다.");
        }

        ReservationParticipant participant = reservationParticipantRepository
                .findAllByReservationId(reservationId).stream()
                .filter(found -> memberId.equals(found.getMemberId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("예약 참석자만 퇴실할 수 있습니다."));

        ReservationAttendance attendance = reservationAttendanceRepository
                .findByReservationParticipantId(participant.getId())
                .orElseThrow(() -> new ConflictException("입실 기록이 없어 퇴실할 수 없습니다."));

        if (attendance.getCheckedOutAt() != null) {
            return attendance;
        }

        LocalDateTime checkoutCloseAt = reservation.getEndAt().plusMinutes(11);

        if (!checkedOutAt.isBefore(checkoutCloseAt)) {
            throw new ConflictException("퇴실 가능한 시간이 지났습니다.");
        }

        attendance.checkOut(checkedOutAt);
        return attendance;
    }

    public AttendanceStatus classifySession(
            LocalDateTime sessionStart,
            ReservationAttendance attendance
    ) {
        if (sessionStart == null) {
            throw new IllegalArgumentException("판정 시간은 필수입니다.");
        }
        if (attendance == null) {
            throw new IllegalArgumentException("입퇴실 정보는 필수입니다.");
        }
        LocalDateTime checkedInAt = attendance.getCheckedInAt();
        LocalDateTime checkedOutAt = attendance.getCheckedOutAt();

        if(checkedOutAt == null){
            throw new IllegalArgumentException("최종 퇴실한 기록만 회차별로 판정할 수 있습니다.");
        }

        LocalDateTime sessionEnd = sessionStart.plusHours(1);

        if(!checkedOutAt.isAfter(sessionStart) || !checkedInAt.isBefore(sessionEnd)){
            return AttendanceStatus.ABSENT;
        }
        if(!checkedInAt.isBefore(sessionStart.plusMinutes(11))){
            return AttendanceStatus.LATE;
        }
        if(checkedOutAt.isBefore(sessionEnd.minusMinutes(10))){
            return AttendanceStatus.LEAVE_EARLY;
        }

        return AttendanceStatus.PRESENT;
    }

    @Transactional(readOnly = true)
    public ReservationAttendanceResponse getMyAttendance(
            UUID studyId,
            UUID reservationId,
            UUID memberId
    ){
        if(studyId == null || reservationId == null || memberId == null){
            throw new IllegalArgumentException("스터디,예약,회원 ID는 필수입니다.");
        }

        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));

        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException("예약에 연결된 모임을 찾을 수 없습니다."));

        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }

        if(reservation.getReservationStatus() != ReservationStatus.CONFIRMED){
            throw new ConflictException("확정된 예약만 출결을 조회할 수 있습니다.");
        }

        ReservationParticipant participant = reservationParticipantRepository
                .findAllByReservationId(reservationId).stream()
                .filter(found ->
                        memberId.equals(found.getMemberId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("본인의 예약 참석 기록만 조회할 수 있습니다."));

        Optional<ReservationAttendance> existing = reservationAttendanceRepository
                .findByReservationParticipantId(participant.getId());

        LocalDateTime checkedInAt = null;
        LocalDateTime checkedOutAt = null;

        if (existing.isPresent()) {
            ReservationAttendance attendance = existing.get();
            checkedInAt = attendance.getCheckedInAt();
            checkedOutAt = attendance.getCheckedOutAt();
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        LocalDateTime checkoutClosesAt = reservation.getEndAt().plusMinutes(11);

        if (checkedOutAt == null && now.isBefore(checkoutClosesAt)) {
            return new ReservationAttendanceResponse(
                    checkedInAt,
                    checkedOutAt,
                    List.of()
            );
        }

        List<ReservationAttendanceResponse.Session> sessions = new ArrayList<>();

        for (LocalDateTime sessionStart = reservation.getStartAt();
             sessionStart.isBefore(reservation.getEndAt());
             sessionStart = sessionStart.plusHours(1)) {

            AttendanceStatus status;

            if (checkedOutAt == null) {
                status = AttendanceStatus.ABSENT;
            } else {
                status = classifySession(sessionStart, existing.get());
            }

            sessions.add(new ReservationAttendanceResponse.Session(sessionStart, status));
        }

        return new ReservationAttendanceResponse(checkedInAt, checkedOutAt, sessions);
    }
}
