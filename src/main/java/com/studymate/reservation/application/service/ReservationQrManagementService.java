package com.studymate.reservation.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.reservation.domain.reservation.Reservation;
import com.studymate.reservation.domain.reservation.ReservationParticipant;
import com.studymate.reservation.domain.reservation.ReservationParticipantRepository;
import com.studymate.reservation.domain.reservation.ReservationQrManagement;
import com.studymate.reservation.domain.reservation.ReservationQrManagementRepository;
import com.studymate.reservation.domain.reservation.ReservationRepository;
import com.studymate.reservation.domain.reservation.ReservationStatus;
import com.studymate.study.domain.Study;
import com.studymate.study.domain.StudyRepository;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.studymate.reservation.domain.reservation.ReservationAttendance;
import com.studymate.reservation.domain.reservation.ReservationAttendanceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationQrManagementService {
    private final ReservationQrManagementRepository reservationQrManagementRepository;
    private final StudyRepository studyRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final MeetingRepository meetingRepository;
    private final ReservationParticipantRepository reservationParticipantRepository;
    private final ReservationAttendanceRepository reservationAttendanceRepository;

    @Transactional
    public void changeDelegate(
            UUID studyId,
            UUID reservationId,
            UUID memberId,
            UUID delegateParticipantId
    ) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }
        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("해당 스터디를 찾을 수 없습니다."));

        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 대리인을 지정할 수 있습니다.");
        }
        if (!study.getLeaderMemberId().equals(member.getId())) {
            throw new ForbiddenException("그룹장만 대리인을 지정할 수 있습니다.");
        }

        if (reservationId == null) {
            throw new IllegalArgumentException("예약ID는 필수입니다.");
        }
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("해당 예약을 찾을 수 없습니다."));

        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException("해당 모임을 찾을 수 없습니다."));

        if (!study.getId().equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }

        if (delegateParticipantId == null) {
            throw new IllegalArgumentException("대리인ID는 필수입니다.");
        }
        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약만 대리인을 지정할 수 있습니다.");
        }

        ReservationQrManagement management = reservationQrManagementRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new NotFoundException("예약의 QR 관리 정보를 찾을 수 없습니다."));

        if (management.getOperatorParticipantId() != null) {
            throw new ConflictException("QR 개설 후에는 담당자 인계를 이용해 주세요.");
        }

        ReservationParticipant delegate = reservationParticipantRepository
                .findAllByReservationId(reservationId).stream()
                .filter(participant -> delegateParticipantId.equals(participant.getId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("해당 예약의 참석자만 대리인으로 지정할 수 있습니다."));

        management.changeDelegate(delegate.getId(), memberId);
    }

    @Transactional
    public void open(UUID studyId, UUID reservationId, UUID memberId) {
        if (studyId == null || reservationId == null || memberId == null) {
            throw new IllegalArgumentException("스터디, 예약, 회원 ID는 필수입니다.");
        }

        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("해당 스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 QR코드를 열 수 있습니다.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("해당 예약을 찾을 수 없습니다."));
        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException("예약에 연결된 모임을 찾을 수 없습니다."));
        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }
        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약만 QR코드를 열 수 있습니다.");
        }

        ReservationParticipant participant = null;
        for (ReservationParticipant candidate :
                reservationParticipantRepository.findAllByReservationId(reservationId)) {
            if (memberId.equals(candidate.getMemberId())) {
                participant = candidate;
                break;
            }
        }
        if (participant == null) {
            throw new ForbiddenException("해당 예약의 참석자만 QR코드를 열 수 있습니다.");
        }

        ReservationQrManagement management = reservationQrManagementRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new NotFoundException("예약의 QR 관리 정보를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        if (now.isBefore(reservation.getStartAt().minusMinutes(10))
                || !now.isBefore(reservation.getEndAt().plusMinutes(11))) {
            throw new ConflictException("QR코드를 열 수 있는 시간이 아닙니다.");
        }

        Optional<ReservationAttendance> attendance = reservationAttendanceRepository
                .findByReservationParticipantId(participant.getId());
        if (attendance.isPresent() && attendance.get().getCheckedOutAt() != null) {
            throw new ConflictException("최종 퇴실 후에는 QR코드를 열 수 없습니다.");
        }

        UUID currentOperatorId = management.getOperatorParticipantId();
        if (currentOperatorId != null) {
            if (!currentOperatorId.equals(participant.getId())) {
                throw new ConflictException("이미 QR 담당자가 있습니다. 인계를 이용해 주세요.");
            }
            return;
        }

        boolean leader = memberId.equals(study.getLeaderMemberId());
        boolean delegate = participant.getId().equals(management.getDelegateParticipantId());
        if (!leader && !delegate) {
            throw new ForbiddenException("그룹장 또는 지정 대리인만 최초 QR을 열 수 있습니다.");
        }

        management.changeOperator(participant.getId(), memberId);
    }

    @Transactional
    public void handover(
            UUID studyId,
            UUID reservationId,
            UUID memberId,
            UUID nextParticipantId
    ){
        if(studyId == null || reservationId == null ||
        memberId == null || nextParticipantId == null){
            throw new IllegalArgumentException("스터디, 예약, 회원, 다음 담당자 ID는 필수입니다.");
        }
        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("해당 스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 QR 담당자를 인계할 수 있습니다.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("해당 예약을 찾을 수 없습니다."));
        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException("예약에 연결된 모임을 찾을 수 없습니다."));
        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }
        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약만 QR 담당자를 인계할 수 있습니다.");
        }

        ReservationParticipant participant = null;
        for (ReservationParticipant candidate :
                reservationParticipantRepository.findAllByReservationId(reservationId)) {
            if (memberId.equals(candidate.getMemberId())) {
                participant = candidate;
                break;
            }
        }
        if (participant == null) {
            throw new ForbiddenException("해당 예약의 참석자만 QR 담당자를 인계할 수 있습니다.");
        }

        ReservationQrManagement management = reservationQrManagementRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new NotFoundException("예약의 QR 관리 정보를 찾을 수 없습니다."));

        if (!participant.getId().equals(management.getOperatorParticipantId())) {
            throw new ForbiddenException("현재 QR 담당자만 인계할 수 있습니다.");
        }
        if (participant.getId().equals(nextParticipantId)) {
            throw new ConflictException("본인에게 인계할 수 없습니다.");
        }

        Optional<ReservationAttendance> currentAttendance = reservationAttendanceRepository
                .findByReservationParticipantId(participant.getId());
        if (currentAttendance.isPresent() && currentAttendance.get().getCheckedOutAt() != null) {
            throw new ConflictException("최종 퇴실 후에는 QR을 인계할 수 없습니다.");
        }

        ReservationParticipant nextParticipant = null;
        for (ReservationParticipant candidate :
                reservationParticipantRepository.findAllByReservationId(reservationId)) {
            if (nextParticipantId.equals(candidate.getId())) {
                nextParticipant = candidate;
                break;
            }
        }
        if (nextParticipant == null) {
            throw new ForbiddenException("해당 예약의 참석자에게만 인계할 수 있습니다.");
        }

        Member nextMember = memberRepository
                .findByIdAndDeletedAtIsNull(nextParticipant.getMemberId())
                .orElseThrow(() -> new NotFoundException("다음 담당자의 회원 정보를 찾을 수 없습니다."));
        if (nextMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원에게만 인계할 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        if (now.isBefore(reservation.getStartAt().minusMinutes(10))
                || !now.isBefore(reservation.getEndAt().plusMinutes(11))) {
            throw new ConflictException("QR을 인계할 수 있는 시간이 아닙니다.");
        }

        Optional<ReservationAttendance> nextAttendance = reservationAttendanceRepository
                .findByReservationParticipantId(nextParticipant.getId());
        if (nextAttendance.isPresent() && nextAttendance.get().getCheckedOutAt() != null) {
            throw new ConflictException("이미 최종 퇴실한 참석자에게는 인계할 수 없습니다.");
        }

        if (now.isBefore(reservation.getStartAt())) {
            boolean leader = nextParticipant.getMemberId().equals(study.getLeaderMemberId());
            boolean delegate = nextParticipant.getId().equals(management.getDelegateParticipantId());
            if (!leader && !delegate) {
                throw new ForbiddenException("모임 시작 전에는 그룹장 또는 지정 대리인에게만 인계할 수 있습니다.");
            }
        } else if (nextAttendance.isEmpty()) {
            throw new ConflictException("모임 중에는 이미 입실한 참석자에게만 인계할 수 있습니다.");
        }

        management.changeOperator(nextParticipant.getId(), memberId);
    }
}
