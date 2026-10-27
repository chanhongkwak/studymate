package com.studymate.reservation.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.meeting.domain.MeetingRsvp;
import com.studymate.meeting.domain.MeetingRsvpRepository;
import com.studymate.meeting.domain.MeetingRsvpStatus;
import com.studymate.meeting.domain.MeetingStatus;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.reservation.application.dto.request.ReservationCreateRequest;
import com.studymate.reservation.application.dto.response.ReservationResponse;
import com.studymate.reservation.domain.ReservationParticipantRepository;
import com.studymate.reservation.domain.branch.Branch;
import com.studymate.reservation.domain.branch.BranchRepository;
import com.studymate.reservation.domain.branch.BranchStatus;
import com.studymate.reservation.domain.reservation.Reservation;
import com.studymate.reservation.domain.reservation.ReservationParticipant;
import com.studymate.reservation.domain.reservation.ReservationRepository;
import com.studymate.reservation.domain.reservation.ReservationStatus;
import com.studymate.reservation.domain.room.Room;
import com.studymate.reservation.domain.room.RoomRepository;
import com.studymate.reservation.domain.room.RoomStatus;
import com.studymate.study.domain.Study;
import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberRepository;
import com.studymate.study.domain.StudyMemberStatus;
import com.studymate.study.domain.StudyRepository;
import com.studymate.study.domain.StudyStatus;
import com.studymate.wallet.application.dto.request.WalletTransactionRequest;
import com.studymate.wallet.application.service.WalletTransactionService;
import com.studymate.wallet.domain.WalletRepository;
import com.studymate.wallet.domain.WalletTransactionType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final ReservationRepository reservationRepository;
    private final ReservationParticipantRepository reservationParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final MemberRepository memberRepository;
    private final StudyRepository studyRepository;
    private final RoomRepository roomRepository;
    private final BranchRepository branchRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final MeetingRsvpRepository meetingRsvpRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionService walletTransactionService;

    @Transactional
    public ReservationResponse create(
            UUID memberId,
            ReservationCreateRequest request
    ) {
        // 1. 요청에 필요한 값이 있는지 확인
        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }
        if (request == null) {
            throw new IllegalArgumentException("예약 요청은 필수입니다.");
        }
        if (request.meetingId() == null) {
            throw new IllegalArgumentException("모임ID는 필수입니다.");
        }
        if (request.roomId() == null) {
            throw new IllegalArgumentException("방ID는 필수입니다.");
        }

        // 2. 소속 스터디 ID만 조회
        UUID studyId = meetingRepository.findStudyIdById(request.meetingId())
                .orElseThrow(() ->
                        new NotFoundException("해당 모임을 찾을 수 없습니다."));

        // 3. Study 잠금 획득
        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() ->
                        new NotFoundException("해당 스터디가 존재하지 않습니다."));

        // 4. 잠금을 얻은 뒤 Meeting 조회
        Meeting meeting = meetingRepository.findById(request.meetingId())
                .orElseThrow(() ->
                        new NotFoundException("해당 모임을 찾을 수 없습니다."));

        // 5. 요청 회원과 현재 그룹장 권한 확인
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() ->
                        new NotFoundException("해당 회원이 존재하지 않습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 상태의 회원만 예약할 수 있습니다.");
        }
        if (!study.getLeaderMemberId().equals(member.getId())) {
            throw new ForbiddenException("그룹장만 예약할 수 있습니다.");
        }
        if (study.getStatus() == StudyStatus.ENDED) {
            throw new ConflictException("종료된 스터디에서는 예약할 수 없습니다.");
        }

        // 6. 이미 성공한 예약이 있는지 확인
        Optional<Reservation> existingReservation =
                reservationRepository.findByMeetingId(meeting.getId());

        if (existingReservation.isPresent()) {
            Reservation existing = existingReservation.get();

            if (existing.getReservationStatus() == ReservationStatus.CANCELED) {
                throw new ConflictException("취소된 예약은 다시 예약할 수 없습니다.");
            }
            if (!existing.getRoomId().equals(request.roomId())) {
                throw new ConflictException("이미 다른 방으로 예약된 모임입니다.");
            }

            List<ReservationParticipant> participants =
                    reservationParticipantRepository
                            .findAllByReservationId(existing.getId());

            // 기존 결과를 반환하고 메서드 종료
            return ReservationResponse.from(existing, participants);
        }

        // 7. 여기부터는 기존 예약이 없는 경우에만 실행
        if (meeting.getStatus() != MeetingStatus.PROPOSED) {
            throw new ConflictException("제안 상태의 모임만 새로 예약할 수 있습니다.");
        }
// 지점부터 잠금 조회
        Branch branch = branchRepository.findByIdForUpdate(meeting.getBranchId())
                .orElseThrow(() ->
                        new NotFoundException("해당 지점을 찾을 수 없습니다."));

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new ConflictException("운영 중인 지점만 예약할 수 있습니다.");
        }

// 그다음 방 잠금 조회
        Room room = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() ->
                        new NotFoundException("해당 방을 찾을 수 없습니다."));

        if (!room.getBranchId().equals(branch.getId())) {
            throw new ConflictException("모임에서 정한 지점의 방만 예약할 수 있습니다.");
        }

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new ConflictException("운영 중인 방만 예약할 수 있습니다.");
        }

// 아래에는 기존 ACTIVE 스터디원 조회부터 그대로 이어짐
// 현재 ACTIVE 스터디원 조회
        List<StudyMember> activeMembers =
                studyMemberRepository.findAllByStudyIdAndStatus(
                        study.getId(),
                        StudyMemberStatus.ACTIVE
                );

// 현재 스터디원인지 빠르게 확인할 수 있도록 회원 ID를 모음
        Set<UUID> activeMemberIds = new HashSet<>();

        for (StudyMember studyMember : activeMembers) {
            activeMemberIds.add(studyMember.getMemberId());
        }

// 해당 모임의 참석·불참 응답 조회
        List<MeetingRsvp> rsvps =
                meetingRsvpRepository.findAllByMeetingId(meeting.getId());

// ACTIVE 스터디원이면서 ATTENDING인 회원만 참석자로 선정
        List<UUID> attendingMemberIds = new ArrayList<>();

        for (MeetingRsvp rsvp : rsvps) {
            boolean isActiveStudyMember =
                    activeMemberIds.contains(rsvp.getMemberId());

            boolean isAttending =
                    rsvp.getStatus() == MeetingRsvpStatus.ATTENDING;

            if (isActiveStudyMember && isAttending) {
                attendingMemberIds.add(rsvp.getMemberId());
            }
        }
// 전체 ACTIVE 스터디원 수와 참석자 수
        int activeMemberCount = activeMemberIds.size();
        int attendingMemberCount = attendingMemberIds.size();

// 과반수: 전체 인원의 절반을 초과해야 함
        if (attendingMemberCount <= activeMemberCount / 2) {
            throw new ConflictException(
                    "현재 ACTIVE 스터디원의 과반수가 참석해야 예약할 수 있습니다."
            );
        }

// 방 정원: 참석자 수가 정원과 같으면 허용
        if (attendingMemberCount > room.getCapacity()) {
            throw new ConflictException("참석 인원이 방 정원을 초과합니다.");
        }

        if (reservationRepository.existsOverlappingReservation(
                room.getId(),
                meeting.getStartAt(),
                meeting.getEndAt()
        )) {
            throw new ConflictException("해당 시간에 이미 예약된 방입니다.");
        }

        // 예약 객체 생성 — 아직 DB에 저장하지 않음
// 생성자에서 시간 검증과 총액 계산을 수행
        Reservation reservation = new Reservation(
                meeting.getId(),
                room.getId(),
                meeting.getStartAt(),
                meeting.getEndAt(),
                room.getHourlyRate(),
                memberId
        );

// 전체 금액을 참석자 수로 나눈 몫과 나머지
        long totalAmount = reservation.getTotalAmount();
        long baseAmount = totalAmount / attendingMemberCount;
        long remainder = totalAmount % attendingMemberCount;

// 원래 참석자 목록은 유지하고, 추첨용 목록을 만들어 섞음
        List<UUID> shuffledMemberIds = new ArrayList<>(attendingMemberIds);
        Collections.shuffle(shuffledMemberIds);

// 회원별 부담액 기록
        Map<UUID, Long> amountByMemberId = new LinkedHashMap<>();

        for (int i = 0; i < shuffledMemberIds.size(); i++) {
            UUID participantMemberId = shuffledMemberIds.get(i);

            long amount = baseAmount;

            // 섞인 목록의 앞에서부터 나머지 수만큼 1 MC 추가
            if (i < remainder) {
                amount += 1;
            }

            if (amount > meeting.getMaxCostPerPerson()) {
                throw new ConflictException("개인별 부담액이 최대 부담액을 초과합니다.");
            }

            amountByMemberId.put(participantMemberId, amount);
        }
// 분담 금액 계산이 모두 끝난 다음 실행

// 예약 ID 확보 — 아직 커밋은 아님
        Reservation savedReservation = reservationRepository.save(reservation);

// 실제 참석자 목록: 뒤에서 참석자 저장에도 사용
        List<UUID> debitMemberIds = new ArrayList<>(amountByMemberId.keySet());
        debitMemberIds.sort(UUID::compareTo);

// 잠금·차감 처리 대상에는 요청한 그룹장도 포함
        List<UUID> processingMemberIds = new ArrayList<>(debitMemberIds);

        if (!processingMemberIds.contains(memberId)) {
            processingMemberIds.add(memberId);
        }

// 그룹장도 다른 회원들과 동일한 순서 규칙으로 처리
        processingMemberIds.sort(UUID::compareTo);

        for (UUID processingMemberId : processingMemberIds) {
            // 불참 그룹장은 분담 Map에 없으므로 0으로 취급
            long amount = amountByMemberId.getOrDefault(processingMemberId, 0L);

            if (amount == 0) {
                // 차감하지 않는 그룹장도 회원 잠금과 활성 검사는 수행
                if (processingMemberId.equals(memberId)) {
                    Member requester =
                            memberRepository.findByIdForUpdate(processingMemberId)
                                    .orElseThrow(() ->
                                            new NotFoundException("회원을 찾을 수 없습니다."));

                    if (requester.getDeletedAt() != null) {
                        throw new NotFoundException("회원을 찾을 수 없습니다.");
                    }

                    if (requester.getStatus() != MemberStatus.ACTIVE) {
                        throw new ForbiddenException(
                                "활성 상태의 회원만 예약할 수 있습니다."
                        );
                    }
                }

                // 0 MC는 지갑 차감과 거래 생성 생략
                continue;
            }

            UUID walletId = walletRepository.findIdByMemberId(processingMemberId)
                    .orElseThrow(() ->
                            new NotFoundException("참석자의 지갑을 찾을 수 없습니다."));

            WalletTransactionRequest debitRequest = new WalletTransactionRequest(
                    walletId,
                    WalletTransactionType.RESERVATION_DEBIT,
                    amount,
                    savedReservation.getId()
            );

            // 1 MC 이상 부담하는 그룹장도 여기서 회원 잠금·활성 검사를 받음
            walletTransactionService.create(debitRequest, memberId);
        }

// 아래에는 기존 savedParticipants 생성과 참석자 저장 반복문을 유지
        List<ReservationParticipant> savedParticipants = new ArrayList<>();

        for (UUID participantMemberId : debitMemberIds) {
            long amount = amountByMemberId.get(participantMemberId);

            ReservationParticipant participant = new ReservationParticipant(
                    savedReservation.getId(),
                    participantMemberId,
                    amount,
                    memberId
            );

            ReservationParticipant savedParticipant =
                    reservationParticipantRepository.save(participant);

            savedParticipants.add(savedParticipant);
        }

// 참석자 저장까지 처리한 뒤 모임 확정
        meeting.confirm(memberId);

        // DB에 변경 사항 반영 — 아직 커밋은 아님
        reservationRepository.flush();

// 최종 확정 검사 시각 기준으로 24시간 제한 확인
        LocalDateTime checkedAt = LocalDateTime.now(KOREA_ZONE);
        LocalDateTime deadline = meeting.getStartAt().minusHours(24);

        if (checkedAt.isAfter(deadline)) {
            throw new ConflictException(
                    "예약은 모임 시작 최소 24시간 전에 확정해야 합니다."
            );
        }

// 예약 및 참석자별 부담액 반환
        return ReservationResponse.from(savedReservation, savedParticipants);
    }
}
