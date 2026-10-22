package com.studymate.meeting.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.meeting.application.dto.request.MeetingCreateRequest;
import com.studymate.meeting.application.dto.request.MeetingUpdateRequest;
import com.studymate.meeting.application.dto.response.MeetingResponse;
import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.meeting.domain.MeetingRsvp;
import com.studymate.meeting.domain.MeetingRsvpRepository;
import com.studymate.meeting.domain.MeetingRsvpStatus;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.reservation.domain.branch.Branch;
import com.studymate.reservation.domain.branch.BranchRepository;
import com.studymate.reservation.domain.branch.BranchStatus;
import com.studymate.study.domain.Study;
import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberRepository;
import com.studymate.study.domain.StudyMemberStatus;
import com.studymate.study.domain.StudyRepository;
import com.studymate.study.domain.StudyStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MemberRepository memberRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final MeetingRsvpRepository meetingRsvpRepository;
    private final BranchRepository branchRepository;

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Transactional
    public MeetingResponse create(UUID studyId, UUID memberId, MeetingCreateRequest request) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }

        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }

        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (!study.getLeaderMemberId().equals(memberId)) {
            throw new ForbiddenException("그룹장만 모임을 생성할 수 있습니다.");
        }

        if (study.getStatus() == StudyStatus.ENDED) {
            throw new ConflictException("이미 종료된 스터디입니다.");
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 일정 생성이 가능합니다.");
        }

        if (request.branchId() == null) {
            throw new IllegalArgumentException("지점 ID는 필수입니다.");
        }

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new NotFoundException("해당 지점을 찾을 수 없습니다."));

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new ConflictException("운영 중인 지점에만 모임을 제안할 수 있습니다.");
        }

        if (request.maxCostPerPerson() == null) {
            throw new IllegalArgumentException("1인당 최대 부담액은 필수입니다.");
        }

        Meeting meeting = new Meeting(
                studyId,
                branch.getId(),
                request.title(),
                request.description(),
                request.maxCostPerPerson(),
                request.startAt(),
                request.endAt(),
                memberId
        );

        if (meetingRepository.existOverlappingMeeting(
                studyId, meeting.getStartAt(), meeting.getEndAt()
        )) {
            throw new ConflictException("시간이 겹치는 모임이 존재합니다.");
        }

        meetingRepository.save(meeting);

        MeetingRsvp leaderRsvp = new MeetingRsvp(
                meeting.getId(),           // 저장한 모임 ID
                study.getLeaderMemberId(), // 응답 대상: 현재 그룹장
                MeetingRsvpStatus.ATTENDING,
                null,                      // 참석이므로 불참 사유 없음
                memberId                   // 생성 처리자
        );

        meetingRsvpRepository.save(leaderRsvp);

        return MeetingResponse.from(meeting);
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(UUID studyId, UUID meetingId, UUID memberId) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디 ID는 필수입니다.");
        }
        if (meetingId == null) {
            throw new IllegalArgumentException("모임 ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("현재 활동 회원이 아닙니다.");
        }

        StudyMember studyMember = studyMemberRepository.findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(studyId,
                        memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원은 스터디에 소속되어 있지 않습니다."));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("모임을 찾을 수 없습니다."));

        if (!meeting.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 모임을 찾을 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        boolean activeParticipant =
                study.getStatus() != StudyStatus.ENDED
                        && studyMember.getStatus() == StudyMemberStatus.ACTIVE;

        boolean completedParticipant =
                study.getStatus() == StudyStatus.ENDED
                        && studyMember.getStatus() == StudyMemberStatus.COMPLETE
                        && !meeting.getEndAt().isAfter(now);

        if (!activeParticipant && !completedParticipant) {
            throw new ForbiddenException("해당 모임을 조회할 권한이 없습니다.");
        }

        return MeetingResponse.from(meeting);
    }

    @Transactional(readOnly = true)
    public Page<MeetingResponse> getMeetings(
            UUID studyId,
            UUID memberId,
            int page,
            int size
    ) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }

        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }

        if (page < 0) {
            throw new IllegalArgumentException("페이지를 다시 입력해주세요.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("사이즈를 다시 입력해주세요.");
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("현재 활동 회원이 아닙니다.");
        }

        StudyMember studyMember = studyMemberRepository
                .findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(studyId, memberId)
                .orElseThrow(() ->
                        new NotFoundException("해당 회원은 스터디에 소속되어 있지 않습니다."));

        boolean activeParticipant =
                study.getStatus() != StudyStatus.ENDED
                        && studyMember.getStatus() == StudyMemberStatus.ACTIVE;

        boolean completedParticipant =
                study.getStatus() == StudyStatus.ENDED
                        && studyMember.getStatus() == StudyMemberStatus.COMPLETE;

        if (!activeParticipant && !completedParticipant) {
            throw new ForbiddenException("모임 목록을 조회할 권한이 없습니다.");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("startAt"),
                        Sort.Order.asc("id")
                )
        );

        Page<Meeting> meetings;

        if (activeParticipant) {
            meetings = meetingRepository.findAllByStudyId(studyId, pageable);
        } else {
            meetings = meetingRepository.findAllByStudyIdAndEndAtLessThanEqual(
                    studyId, LocalDateTime.now(KOREA_ZONE), pageable
            );
        }

        return meetings.map(MeetingResponse::from);
    }

    @Transactional
    public MeetingResponse update(
            UUID studyId,
            UUID meetingId,
            UUID memberId,
            MeetingUpdateRequest request
    ) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디 ID는 필수입니다.");
        }
        if (meetingId == null) {
            throw new IllegalArgumentException("모임 ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 모임을 수정할 수 있습니다.");
        }

        if (!study.getLeaderMemberId().equals(memberId)) {
            throw new ForbiddenException("그룹장만 모임을 수정할 수 있습니다.");
        }

        if (study.getStatus() == StudyStatus.ENDED) {
            throw new ConflictException("종료된 스터디의 모임은 수정할 수 없습니다.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("모임을 찾을 수 없습니다."));

        if (!meeting.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 모임을 찾을 수 없습니다.");
        }

        if (request.branchId() != null
                && !request.branchId().equals(meeting.getBranchId())) {

            Branch branch = branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new NotFoundException("해당 지점을 찾을 수 없습니다."));

            if (branch.getStatus() != BranchStatus.ACTIVE) {
                throw new ConflictException("운영 중인 지점으로만 변경할 수 있습니다.");
            }
        }

        UUID previousBranchId = meeting.getBranchId();
        LocalDateTime previousStartAt = meeting.getStartAt();
        LocalDateTime previousEndAt = meeting.getEndAt();
        long previousMaxCostPerPerson = meeting.getMaxCostPerPerson();

        meeting.update(
                request.branchId(),
                request.title(),
                request.description(),
                request.maxCostPerPerson(),
                request.startAt(),
                request.endAt(),
                memberId
        );

        if (meetingRepository.existsOverlappingMeetingExcludingId(
                studyId,
                meetingId,
                meeting.getStartAt(),
                meeting.getEndAt()
        )) {
            throw new ConflictException("시간이 겹치는 모임이 존재합니다.");
        }

        boolean reservationConditionsChanged =
                !previousBranchId.equals(meeting.getBranchId())
                        || !previousStartAt.equals(meeting.getStartAt())
                        || !previousEndAt.equals(meeting.getEndAt())
                        || previousMaxCostPerPerson != meeting.getMaxCostPerPerson();

        if (reservationConditionsChanged) {
            meetingRsvpRepository.deleteByMeetingId(meetingId);
        }

        return MeetingResponse.from(meeting);
    }

    @Transactional
    public MeetingResponse cancel(
            UUID studyId,
            UUID meetingId,
            UUID memberId
    ) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디 ID는 필수입니다.");
        }
        if (meetingId == null) {
            throw new IllegalArgumentException("모임 ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("스터디는 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 모임을 취소할 수 있습니다.");
        }

        if (!study.getLeaderMemberId().equals(memberId)) {
            throw new ForbiddenException("그룹장만 모임을 취소할 수 있습니다.");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("모임을 찾을 수 없습니다."));

        if (!meeting.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 모임을 찾을 수 없습니다.");
        }

        meeting.cancel(memberId);

        return MeetingResponse.from(meeting);
    }

}
