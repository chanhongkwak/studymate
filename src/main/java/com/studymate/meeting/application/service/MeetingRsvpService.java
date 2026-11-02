package com.studymate.meeting.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.meeting.application.dto.request.MeetingRsvpCreateRequest;
import com.studymate.meeting.application.dto.request.MeetingRsvpUpdateRequest;
import com.studymate.meeting.application.dto.response.MeetingRsvpResponse;
import com.studymate.meeting.application.dto.response.MeetingRsvpSummaryResponse;
import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingParticipant;
import com.studymate.meeting.domain.MeetingParticipantRepository;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.meeting.domain.MeetingRsvp;
import com.studymate.meeting.domain.MeetingRsvpRepository;
import com.studymate.meeting.domain.MeetingRsvpStatus;
import com.studymate.meeting.domain.MeetingStatus;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.study.domain.Study;
import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberRepository;
import com.studymate.study.domain.StudyMemberStatus;
import com.studymate.study.domain.StudyRepository;
import com.studymate.study.domain.StudyStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MeetingRsvpService {
    private final MeetingRsvpRepository meetingRsvpRepository;
    private final StudyRepository studyRepository;
    private final MeetingRepository meetingRepository;
    private final MemberRepository memberRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingResponseService meetingResponseService;

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Transactional
    public MeetingRsvpResponse create(UUID studyId, UUID meetingId, UUID memberId, MeetingRsvpCreateRequest request) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }
        if (meetingId == null) {
            throw new IllegalArgumentException("모임ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("멤버ID는 필수입니다.");
        }
        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        if (study.getStatus() == StudyStatus.ENDED) {
            throw new ConflictException("종료된 스터디에는 참석 응답을 변경할 수 없습니다.");
        }
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 참석 응답이 가능합니다.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("해당 모임을 찾을 수 없습니다."));

        if (!meeting.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 모임을 찾을 수 없습니다.");
        }

        if (meeting.getStatus() != MeetingStatus.PROPOSED) {
            throw new ConflictException(
                    "제안 상태의 모임에만 참석 응답을 할 수 있습니다."
            );
        }

        if (meeting.getResponseClosedAt() != null) {
            throw new ConflictException("이미 응답이 종료된 모임입니다.");
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        if (!meeting.getStartAt().isAfter(now)) {
            throw new ConflictException(
                    "이미 시작한 모임에는 참석 응답을 할 수 없습니다."
            );
        }

        if (meeting.getResponseDeadlineAt() == null || !now.isBefore(meeting.getResponseDeadlineAt())) {
            throw new ConflictException("응답 마감 시각이 지났습니다.");
        }

        StudyMember studyMember = studyMemberRepository.findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(
                studyId, memberId
        ).orElseThrow(() -> new NotFoundException("해당 스터디회원을 찾을 수 없습니다."));

        if (studyMember.getStatus() != StudyMemberStatus.ACTIVE) {
            throw new ForbiddenException("현재 스터디 참여자만 참석 응답이 가능합니다.");
        }

        MeetingParticipant participant = meetingParticipantRepository
                .findByMeetingIdAndMemberId(meetingId, memberId)
                .orElseThrow(() ->
                        new ForbiddenException("해당 모임의 응답 대상자가 아닙니다."));

        if (!participant.getStudyMemberId().equals(studyMember.getId())) {
            throw new ForbiddenException(
                    "재가입 이전에 제안된 모임에는 응답할 수 없습니다."
            );
        }

        if (meetingRsvpRepository
                .findByMeetingIdAndMemberId(meetingId, memberId)
                .isPresent()) {
            throw new ConflictException("이미 참석 응답이 존재합니다.");
        }

        MeetingRsvp meetingRsvp = new MeetingRsvp(
                meetingId,
                memberId,
                request.status(),
                request.reason(),
                memberId
        );

        meetingRsvpRepository.save(meetingRsvp);

        meetingResponseService.closeIfAllResponded(meeting, memberId);

        return MeetingRsvpResponse.from(meetingRsvp);
    }

    @Transactional
    public MeetingRsvpResponse update(UUID studyId, UUID meetingId, UUID memberId, MeetingRsvpUpdateRequest request) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }
        if (meetingId == null) {
            throw new IllegalArgumentException("모임ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("멤버ID는 필수입니다.");
        }
        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        if (study.getStatus() == StudyStatus.ENDED) {
            throw new ConflictException("종료된 스터디에는 참석 응답을 변경할 수 없습니다.");
        }
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 참석 응답이 가능합니다.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("해당 모임을 찾을 수 없습니다."));

        if (!meeting.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 모임을 찾을 수 없습니다.");
        }

        if (meeting.getStatus() != MeetingStatus.PROPOSED) {
            throw new ConflictException(
                    "제안 상태의 모임에만 참석 응답을 할 수 있습니다."
            );
        }

        if (meeting.getResponseClosedAt() != null) {
            throw new ConflictException("이미 응답이 종료된 모임입니다.");
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        if (!meeting.getStartAt().isAfter(now)) {
            throw new ConflictException(
                    "이미 시작한 모임에는 참석 응답을 할 수 없습니다."
            );
        }

        if (meeting.getResponseDeadlineAt() == null || !now.isBefore(meeting.getResponseDeadlineAt())) {
            throw new ConflictException("응답 마감 시각이 지났습니다.");
        }

        StudyMember studyMember = studyMemberRepository.findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(
                studyId, memberId
        ).orElseThrow(() -> new NotFoundException("해당 스터디회원을 찾을 수 없습니다."));

        if (studyMember.getStatus() != StudyMemberStatus.ACTIVE) {
            throw new ForbiddenException("현재 스터디 참여자만 참석 응답이 가능합니다.");
        }

        MeetingParticipant participant = meetingParticipantRepository
                .findByMeetingIdAndMemberId(meetingId, memberId)
                .orElseThrow(() ->
                        new ForbiddenException("해당 모임의 응답 대상자가 아닙니다."));

        if (!participant.getStudyMemberId().equals(studyMember.getId())) {
            throw new ForbiddenException(
                    "재가입 이전에 제안된 모임에는 응답할 수 없습니다."
            );
        }

        MeetingRsvp meetingRsvp = meetingRsvpRepository.findByMeetingIdAndMemberId(meetingId, memberId)
                .orElseThrow(() -> new NotFoundException("해당 모임 참석 여부를 확인 할 수 없습니다."));

        meetingRsvp.update(
                request.status(),
                request.reason(),
                memberId
        );

        meetingResponseService.closeIfAllResponded(meeting, memberId);

        return MeetingRsvpResponse.from(meetingRsvp);
    }

    @Transactional(readOnly = true)
    public MeetingRsvpResponse getMyRsvp(
            UUID studyId,
            UUID meetingId,
            UUID memberId
    ) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }
        if (meetingId == null) {
            throw new IllegalArgumentException("모임ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 참석 응답을 조회할 수 있습니다.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("해당 모임을 찾을 수 없습니다."));

        if (!meeting.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 모임을 찾을 수 없습니다.");
        }

        StudyMember studyMember = studyMemberRepository
                .findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(studyId, memberId)
                .orElseThrow(() -> new NotFoundException("해당 스터디회원을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        boolean activeParticipant = study.getStatus() != StudyStatus.ENDED
                && studyMember.getStatus() == StudyMemberStatus.ACTIVE;

        boolean completedParticipant = study.getStatus() == StudyStatus.ENDED
                && studyMember.getStatus() == StudyMemberStatus.COMPLETE
                && !meeting.getEndAt().isAfter(now);

        if (!activeParticipant && !completedParticipant) {
            throw new ForbiddenException("참석 응답을 조회할 권한이 없습니다.");
        }

        if (meetingParticipantRepository.findByMeetingIdAndMemberId(meetingId, memberId).isEmpty()
                && meeting.getResponseDeadlineAt() != null) {
            throw new ForbiddenException("해당 모임의 응답 대상자가 아닙니다.");
        }
        return meetingRsvpRepository.findByMeetingIdAndMemberId(meetingId, memberId)
                .map(MeetingRsvpResponse::from)
                .orElseGet(() -> MeetingRsvpResponse.unanswered(meetingId, memberId));
    }

    @Transactional(readOnly = true)
    public MeetingRsvpSummaryResponse getRsvpSummary(
            UUID studyId,
            UUID meetingId,
            UUID memberId
    ) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }
        if (meetingId == null) {
            throw new IllegalArgumentException("모임ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 참석 응답을 조회할 수 있습니다.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("해당 모임을 찾을 수 없습니다."));

        if (!meeting.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 모임을 찾을 수 없습니다.");
        }

        StudyMember studyMember = studyMemberRepository
                .findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(studyId, memberId)
                .orElseThrow(() -> new NotFoundException("해당 스터디회원을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        boolean activeParticipant = study.getStatus() != StudyStatus.ENDED
                && studyMember.getStatus() == StudyMemberStatus.ACTIVE;

        boolean completedParticipant = study.getStatus() == StudyStatus.ENDED
                && studyMember.getStatus() == StudyMemberStatus.COMPLETE
                && !meeting.getEndAt().isAfter(now);

        if (!activeParticipant && !completedParticipant) {
            throw new ForbiddenException("참석 응답을 조회할 권한이 없습니다.");
        }

        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingId(meetingId);
        Set<UUID> activeMemberships = new HashSet<>();
        if (meeting.getResponseClosedAt() == null) {
            for (StudyMember membership : studyMemberRepository.findAllByStudyIdAndStatus(
                    studyId, StudyMemberStatus.ACTIVE)) {
                activeMemberships.add(membership.getId());
            }
        }

        List<MeetingRsvp> rsvps = meetingRsvpRepository.findAllByMeetingId(meetingId);

        // Older meetings have recorded answers but no original voter roster.
        if (meeting.getResponseDeadlineAt() == null && meeting.getCancellationReason() == null) {
            int attending = (int) rsvps.stream()
                    .filter(rsvp -> rsvp.getStatus() == MeetingRsvpStatus.ATTENDING).count();
            return new MeetingRsvpSummaryResponse(meetingId, attending, rsvps.size() - attending,
                    null, rsvps.stream().map(MeetingRsvpResponse::from).toList());
        }
        Map<UUID, MeetingRsvp> rsvpByMemberId = new HashMap<>();

        for (MeetingRsvp rsvp : rsvps) {
            rsvpByMemberId.put(rsvp.getMemberId(), rsvp);
        }

        List<MeetingRsvpResponse> members = new ArrayList<>();

        int attendingCount = 0;
        int notAttendingCount = 0;
        int unansweredCount = 0;

        for (MeetingParticipant participant : participants) {
            boolean eligible = meeting.getResponseClosedAt() == null
                    ? activeMemberships.contains(participant.getStudyMemberId()) : !participant.isExcludedAtClosure();
            if (!eligible || meeting.getCancellationReason() != null) {
                continue;
            }
            UUID participantMemberId = participant.getMemberId();
            MeetingRsvp rsvp = rsvpByMemberId.get(participantMemberId);

            if (rsvp == null) {
                members.add(MeetingRsvpResponse.unanswered(
                        meetingId,
                        participantMemberId
                ));
                unansweredCount++;
            } else {
                members.add(MeetingRsvpResponse.from(rsvp));

                if (rsvp.getStatus() == MeetingRsvpStatus.ATTENDING) {
                    attendingCount++;
                } else {
                    notAttendingCount++;
                }
            }
        }

        return new MeetingRsvpSummaryResponse(
                meetingId,
                attendingCount,
                notAttendingCount,
                unansweredCount,
                members
        );
    }
}
