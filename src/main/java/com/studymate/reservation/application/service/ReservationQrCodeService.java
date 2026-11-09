package com.studymate.reservation.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.reservation.application.dto.response.ReservationQrCodeResponse;
import com.studymate.reservation.application.dto.response.ReservationQrScanResponse;
import com.studymate.reservation.domain.reservation.AttendanceAction;
import com.studymate.reservation.domain.reservation.Reservation;
import com.studymate.reservation.domain.reservation.ReservationAttendance;
import com.studymate.reservation.domain.reservation.ReservationAttendanceGrant;
import com.studymate.reservation.domain.reservation.ReservationAttendanceGrantRepository;
import com.studymate.reservation.domain.reservation.ReservationAttendanceRepository;
import com.studymate.reservation.domain.reservation.ReservationParticipant;
import com.studymate.reservation.domain.reservation.ReservationParticipantRepository;
import com.studymate.reservation.domain.reservation.ReservationQrCode;
import com.studymate.reservation.domain.reservation.ReservationQrCodeRepository;
import com.studymate.reservation.domain.reservation.ReservationQrManagement;
import com.studymate.reservation.domain.reservation.ReservationQrManagementRepository;
import com.studymate.reservation.domain.reservation.ReservationRepository;
import com.studymate.reservation.domain.reservation.ReservationStatus;
import com.studymate.study.domain.StudyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationQrCodeService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudyRepository studyRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final MeetingRepository meetingRepository;
    private final ReservationParticipantRepository reservationParticipantRepository;
    private final ReservationAttendanceRepository reservationAttendanceRepository;
    private final ReservationQrManagementRepository reservationQrManagementRepository;
    private final ReservationQrCodeRepository reservationQrCodeRepository;
    private final ReservationAttendanceGrantRepository reservationAttendanceGrantRepository;
    private final ReservationAttendanceService reservationAttendanceService;

    @Transactional
    public ReservationQrCodeResponse issue(UUID studyId, UUID reservationId, UUID memberId) {
        if (studyId == null || reservationId == null || memberId == null) {
            throw new IllegalArgumentException("스터디, 예약, 회원 ID는 필수입니다.");
        }
        studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 QR을 발급할 수 있습니다.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));
        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException("예약의 모임을 찾을 수 없습니다."));
        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }
        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약만 QR을 발급할 수 있습니다.");
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
            throw new ForbiddenException("예약 참석자만 QR을 발급할 수 있습니다.");
        }

        ReservationQrManagement management = reservationQrManagementRepository
                .findByReservationId(reservationId)
                .orElseThrow(() -> new NotFoundException("QR 관리 정보를 찾을 수 없습니다."));
        if (!participant.getId().equals(management.getOperatorParticipantId())) {
            throw new ForbiddenException("현재 QR 담당자만 QR을 발급할 수 있습니다.");
        }

        Optional<ReservationAttendance> attendance = reservationAttendanceRepository
                .findByReservationParticipantId(participant.getId());
        if (attendance.isPresent() && attendance.get().getCheckedOutAt() != null) {
            throw new ConflictException("최종 퇴실 후에는 QR을 발급할 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        if (now.isBefore(reservation.getStartAt().minusMinutes(10))
                || !now.isBefore(reservation.getEndAt().plusMinutes(11))) {
            throw new ConflictException("QR을 발급할 수 있는 시간이 아닙니다.");
        }

        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        ReservationQrCode qrCode = new ReservationQrCode(
                reservationId, hashToken(token), participant.getId());
        if (!qrCode.getIssuedAt().isBefore(reservation.getEndAt().plusMinutes(11))) {
            throw new ConflictException("QR 발급 마감 시각이 지났습니다.");
        }
        reservationQrCodeRepository.save(qrCode);

        return new ReservationQrCodeResponse(token, qrCode.getIssuedAt(), qrCode.getExpiredAt());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public ReservationQrCode validateScan(
            UUID reservationId,
            UUID memberId,
            String token
    ) {
        if (reservationId == null || memberId == null) {
            throw new IllegalArgumentException("예약ID , 회원ID는 필수입니다.");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("QR 토큰은 필수입니다.");
        }

        String tokenHash = hashToken(token);

        ReservationQrCode qrCode = reservationQrCodeRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new NotFoundException("유효한 QR코드를 찾을 수 없습니다."));

        if (!reservationId.equals(qrCode.getReservationId())) {
            throw new ForbiddenException("해당 예약의 QR코드가 아닙니다.");
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        if (!now.isBefore(qrCode.getExpiredAt())) {
            throw new ConflictException("QR코드가 만료됐습니다. 다시 스캔해 주세요");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 QR코드를 사용할 수 있습니다.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));

        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약의 QR코드만 사용할 수 있습니다.");
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
            throw new ForbiddenException("예약 참석자만 QR코드를 사용할 수 있습니다.");
        }

        return qrCode;
    }

    @Transactional
    public ReservationQrScanResponse scan(
            UUID studyId,
            UUID reservationId,
            UUID memberId,
            String token
    ) {
        if (studyId == null || reservationId == null || memberId == null) {
            throw new IllegalArgumentException("스터디, 예약, 회원 ID는 필수입니다.");
        }

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("QR 토큰은 필수입니다.");
        }

        studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));

        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException("예약의 모임을 찾을 수 없습니다."));

        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }

        ReservationQrCode qrCode = validateScan(
                reservationId,
                memberId,
                token
        );

        ReservationParticipant participant = null;

        for (ReservationParticipant candidate :
                reservationParticipantRepository.findAllByReservationId(reservationId)
        ) {
            if (memberId.equals(candidate.getMemberId())) {
                participant = candidate;
                break;
            }
        }

        if (participant == null) {
            throw new ForbiddenException("예약 참석자만 QR코드를 사용할 수 있습니다.");
        }

        Optional<ReservationAttendance> attendance = reservationAttendanceRepository
                .findByReservationParticipantId(participant.getId());

        AttendanceAction action;

        if (attendance.isEmpty()) {
            action = AttendanceAction.CHECK_IN;
        } else {
            ReservationAttendance existing = attendance.get();

            if (existing.getCheckedOutAt() != null) {
                return new ReservationQrScanResponse(
                        null,
                        null,
                        null,
                        existing.getCheckedOutAt()
                );
            }

            action = AttendanceAction.CHECK_OUT;
        }

        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);

        String grantToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        ReservationAttendanceGrant grant = new ReservationAttendanceGrant(
                participant.getId(),
                hashToken(grantToken),
                action
        );

        LocalDateTime issuedAt = grant.getIssuedAt();

        if (action == AttendanceAction.CHECK_IN) {
            LocalDateTime opensAt = reservation.getStartAt().minusMinutes(10);
            LocalDateTime closesAt = reservation.getEndAt().minusMinutes(10);

            if (issuedAt.isBefore(opensAt) || !issuedAt.isBefore(closesAt)) {
                throw new ConflictException("입실 가능한 시간이 아닙니다.");
            }
        } else {
            LocalDateTime closesAt = reservation.getEndAt().plusMinutes(11);

            if (!issuedAt.isBefore(closesAt)) {
                throw new ConflictException("퇴실 가능한 시간이 지났습니다.");
            }
        }

        reservationAttendanceGrantRepository.save(grant);

        return new ReservationQrScanResponse(
                grantToken,
                grant.getAction(),
                grant.getExpiresAt(),
                null
        );
    }

    @Transactional
    public ReservationAttendance checkInWithGrant(
            UUID studyId,
            UUID reservationId,
            UUID memberId,
            String grantToken
    ) {
        return executeWithGrant(
                studyId,
                reservationId,
                memberId,
                grantToken,
                AttendanceAction.CHECK_IN
        );
    }

    @Transactional
    public ReservationAttendance checkOutWithGrant(
            UUID studyId,
            UUID reservationId,
            UUID memberId,
            String grantToken
    ) {
        return executeWithGrant(
                studyId,
                reservationId,
                memberId,
                grantToken,
                AttendanceAction.CHECK_OUT
        );
    }

    @Transactional
    public ReservationAttendance checkInAsOperator(
            UUID studyId,
            UUID reservationId,
            UUID memberId
    ) {
        return executeAsOperator(
                studyId,
                reservationId,
                memberId,
                AttendanceAction.CHECK_IN
        );
    }

    @Transactional
    public ReservationAttendance checkOutAsOperator(
            UUID studyId,
            UUID reservationId,
            UUID memberId
    ) {
        return executeAsOperator(
                studyId,
                reservationId,
                memberId,
                AttendanceAction.CHECK_OUT
        );
    }

    private ReservationAttendance executeAsOperator(
            UUID studyId,
            UUID reservationId,
            UUID memberId,
            AttendanceAction action
    ) {
        if (studyId == null || reservationId == null || memberId == null) {
            throw new IllegalArgumentException(
                    "스터디, 예약, 회원 ID는 필수입니다."
            );
        }

        studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException(
                        "스터디를 찾을 수 없습니다."
                ));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException(
                        "회원을 찾을 수 없습니다."
                ));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 입퇴실할 수 있습니다.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(
                        "예약을 찾을 수 없습니다."
                ));

        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException(
                        "예약의 모임을 찾을 수 없습니다."
                ));

        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }

        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약만 입퇴실할 수 있습니다.");
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
            throw new ForbiddenException("예약 참석자만 입퇴실할 수 있습니다.");
        }

        ReservationQrManagement management =
                reservationQrManagementRepository.findByReservationId(reservationId)
                        .orElseThrow(() -> new NotFoundException(
                                "예약의 QR 관리 정보를 찾을 수 없습니다."
                        ));

        if (!participant.getId().equals(management.getOperatorParticipantId())) {
            throw new ForbiddenException(
                    "현재 QR 담당자만 스캔 없이 본인 입퇴실을 처리할 수 있습니다."
            );
        }

        if (action == AttendanceAction.CHECK_IN) {
            return reservationAttendanceService.checkIn(
                    studyId,
                    reservationId,
                    memberId
            );
        } else {
            return reservationAttendanceService.checkOut(
                    studyId,
                    reservationId,
                    memberId
            );
        }
    }

    private ReservationAttendance executeWithGrant(
            UUID studyId,
            UUID reservationId,
            UUID memberId,
            String grantToken,
            AttendanceAction expectedAction
    ) {
        if (studyId == null || reservationId == null || memberId == null) {
            throw new IllegalArgumentException(
                    "스터디, 예약, 회원 ID는 필수입니다."
            );
        }

        if (grantToken == null || grantToken.isBlank()) {
            throw new IllegalArgumentException("개인 권한 토큰은 필수입니다.");
        }

        studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException(
                        "스터디를 찾을 수 없습니다."
                ));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException(
                        "회원을 찾을 수 없습니다."
                ));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 입퇴실할 수 있습니다.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException(
                        "예약을 찾을 수 없습니다."
                ));

        Meeting meeting = meetingRepository.findById(reservation.getMeetingId())
                .orElseThrow(() -> new NotFoundException(
                        "예약의 모임을 찾을 수 없습니다."
                ));

        if (!studyId.equals(meeting.getStudyId())) {
            throw new NotFoundException("해당 스터디의 예약을 찾을 수 없습니다.");
        }

        if (reservation.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new ConflictException("확정된 예약만 입퇴실할 수 있습니다.");
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
            throw new ForbiddenException("예약 참석자만 입퇴실할 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        ReservationAttendanceGrant grant = validateGrant(
                grantToken,
                participant.getId(),
                expectedAction,
                now
        );

        ReservationAttendance attendance;

        if (expectedAction == AttendanceAction.CHECK_IN) {
            attendance = reservationAttendanceService.checkIn(
                    studyId,
                    reservationId,
                    memberId
            );
        } else {
            attendance = reservationAttendanceService.checkOut(
                    studyId,
                    reservationId,
                    memberId
            );
        }

        grant.use(now);

        return attendance;
    }

    private ReservationAttendanceGrant validateGrant(
            String grantToken,
            UUID participantId,
            AttendanceAction expectAction,
            LocalDateTime now
    ){
        if(grantToken == null || grantToken.isBlank()){
            throw new IllegalArgumentException("개인 권한 토큰은 필수입니다.");
        }

        ReservationAttendanceGrant grant = reservationAttendanceGrantRepository
                .findByTokenHash(hashToken(grantToken))
                .orElseThrow(() -> new NotFoundException("개인 출결 권한을 찾을 수 없습니다."));

        if(!participantId.equals(grant.getReservationParticipantId())){
            throw new ForbiddenException("본인의 예약 출결 권한만 사용할 수 있습니다.");
        }

        if (grant.getAction() != expectAction) {
            throw new ForbiddenException("요청한 입퇴실 용도와 다른 권한입니다.");
        }

        if(grant.getUsedAt() != null){
            throw new ConflictException("이미 사용한 권한입니다.");
        }

        if (now.isBefore(grant.getIssuedAt())) {
            throw new ConflictException("아직 사용할 수 없는 권한입니다.");
        }

        if(!now.isBefore(grant.getExpiresAt())){
            throw new ConflictException("개인 출결 권한이 만료됐습니다. 다시 스캔해 주세요.");
        }

        return grant;
    }
}
