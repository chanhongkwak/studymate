package com.studymate.meeting.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingParticipant;
import com.studymate.meeting.domain.MeetingParticipantRepository;
import com.studymate.meeting.domain.MeetingRsvp;
import com.studymate.meeting.domain.MeetingRsvpRepository;
import com.studymate.meeting.domain.MeetingRsvpStatus;
import com.studymate.meeting.domain.MeetingStatus;
import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberRepository;
import com.studymate.study.domain.StudyMemberStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingResponseService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final UUID SYSTEM_ACTOR_ID = new UUID(0L, 0L);

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRsvpRepository meetingRsvpRepository;
    private final StudyMemberRepository studyMemberRepository;

    private ResponseCounts countResponses(Meeting meeting, List<MeetingParticipant> excluded) {
        if (meeting == null || meeting.getId() == null) {
            throw new IllegalArgumentException("저장된 모임이 필요합니다.");
        }

        List<MeetingParticipant> participants =
                meetingParticipantRepository.findAllByMeetingId(
                        meeting.getId()
                );

        List<StudyMember> activeMembers =
                studyMemberRepository.findAllByStudyIdAndStatus(
                        meeting.getStudyId(),
                        StudyMemberStatus.ACTIVE
                );

        Map<UUID, StudyMember> activeMemberById = new HashMap<>();

        for (StudyMember studyMember : activeMembers) {
            activeMemberById.put(studyMember.getId(), studyMember);
        }

        List<MeetingRsvp> rsvps =
                meetingRsvpRepository.findAllByMeetingId(meeting.getId());

        Map<UUID, MeetingRsvp> rsvpByMemberId = new HashMap<>();

        for (MeetingRsvp rsvp : rsvps) {
            rsvpByMemberId.put(rsvp.getMemberId(), rsvp);
        }

        int targetCount = 0;
        int attendingCount = 0;
        int notAttendingCount = 0;

        for (MeetingParticipant participant : participants) {
            StudyMember activeMember =
                    activeMemberById.get(participant.getStudyMemberId());

            if (activeMember == null) {
                excluded.add(participant);
                continue;
            }

            if (!activeMember.getMemberId().equals(participant.getMemberId())) {
                throw new IllegalStateException(
                        "모임 대상자의 회원 정보와 가입 기록이 일치하지 않습니다."
                );
            }

            targetCount++;

            MeetingRsvp rsvp =
                    rsvpByMemberId.get(participant.getMemberId());

            if (rsvp == null) {
                continue;
            }

            if (rsvp.getStatus() == MeetingRsvpStatus.ATTENDING) {
                attendingCount++;
            } else if (rsvp.getStatus() == MeetingRsvpStatus.NOT_ATTENDING) {
                notAttendingCount++;
            }
        }

        return new ResponseCounts(
                targetCount,
                attendingCount,
                notAttendingCount
        );
    }

    public record ResponseCounts(
            int targetCount,
            int attendingCount,
            int notAttendingCount
    ) {
        public int respondedCount() {
            return attendingCount + notAttendingCount;
        }

        public int unansweredCount() {
            return targetCount - respondedCount();
        }
    }

    // 호출하는 Service가 먼저 해당 Study 잠금을 확보해야 한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeIfAllResponded(Meeting meeting, UUID updatedBy) {
        if (meeting == null || meeting.getId() == null) {
            throw new IllegalArgumentException("저장된 모임이 필요합니다.");
        }

        if (updatedBy == null) {
            throw new IllegalArgumentException("처리자 ID는 필수입니다.");
        }

        if (meeting.getStatus() != MeetingStatus.PROPOSED) {
            throw new ConflictException(
                    "제안 상태의 모임만 응답 종료를 판단할 수 있습니다."
            );
        }

        if (meeting.getResponseClosedAt() != null) {
            return;
        }

        List<MeetingParticipant> excluded = new ArrayList<>();
        ResponseCounts counts = countResponses(meeting, excluded);

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        if (!now.isBefore(meeting.getResponseDeadlineAt())) {
            throw new ConflictException("응답 마감 시각이 지났습니다.");
        }

        if (counts.targetCount() == 0) {
            throw new ConflictException("응답 가능한 대상자가 없습니다.");
        }

        if (counts.respondedCount() != counts.targetCount()) {
            return;
        }

        boolean majorityApproved =
                counts.attendingCount() > counts.targetCount() / 2;

        excluded.forEach(MeetingParticipant::excludeAtClosure);
        meeting.closeResponse(now, majorityApproved, updatedBy);
    }

    // Study 잠금 안에서, 해당 가입 기록을 LEFT로 변경한 직후 호출한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeIfAllRespondedAfterLeave(
            Meeting meeting,
            LocalDateTime leaveDecisionAt,
            UUID memberId
    ) {
        if (meeting.getStatus() != MeetingStatus.PROPOSED
                || meeting.getResponseClosedAt() != null
                || !leaveDecisionAt.isBefore(meeting.getResponseDeadlineAt())) {
            return;
        }

        List<MeetingParticipant> excluded = new ArrayList<>();
        ResponseCounts counts = countResponses(meeting, excluded);

        if (counts.targetCount() == 0 || counts.unansweredCount() > 0) {
            return;
        }

        boolean majorityApproved =
                counts.attendingCount() > counts.targetCount() / 2;

        excluded.forEach(MeetingParticipant::excludeAtClosure);
        meeting.closeResponse(leaveDecisionAt, majorityApproved, memberId);
    }

    // 호출하는 Service가 먼저 해당 Study 잠금을 확보해야 한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeIfDeadlineReached(Meeting meeting, UUID updatedBy) {
        if (meeting == null || meeting.getId() == null) {
            throw new IllegalArgumentException("저장된 모임이 필요합니다.");
        }

        if (meeting.getStatus() != MeetingStatus.PROPOSED
                || meeting.getResponseClosedAt() != null) {
            return;
        }

        LocalDateTime deadline = meeting.getResponseDeadlineAt();
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        if (deadline == null || now.isBefore(deadline)) {
            return;
        }

        if (updatedBy == null) {
            throw new IllegalArgumentException("처리자 ID는 필수입니다.");
        }

        List<MeetingParticipant> excluded = new ArrayList<>();
        ResponseCounts counts = countResponses(meeting, excluded);
        boolean majorityApproved =
                counts.attendingCount() > counts.targetCount() / 2;

        excluded.forEach(MeetingParticipant::excludeAtClosure);
        meeting.closeResponse(deadline, majorityApproved, updatedBy);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void closeIfDeadlineReachedBySystem(Meeting meeting) {
        closeIfDeadlineReached(meeting, SYSTEM_ACTOR_ID);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void expireUnreserved(Meeting meeting, LocalDateTime now) {
        meeting.expireUnreserved(now, SYSTEM_ACTOR_ID);
        meetingRsvpRepository.deleteByMeetingId(meeting.getId());
    }
}
