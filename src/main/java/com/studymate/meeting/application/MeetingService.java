package com.studymate.meeting.application;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.meeting.application.dto.request.MeetingCreateRequest;
import com.studymate.meeting.application.dto.request.MeetingUpdateRequest;
import com.studymate.meeting.application.dto.response.MeetingResponse;
import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
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

        Meeting meeting = new Meeting(
                studyId,
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.location(),
                memberId
        );

        if (meetingRepository.existOverlappingMeeting(
                studyId, meeting.getStartAt(), meeting.getEndAt()
        )) {
            throw new ConflictException("시간이 겹치는 모임이 존재합니다.");
        }

        meetingRepository.save(meeting);
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

        LocalDateTime now = LocalDateTime.now();

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
                    studyId, LocalDateTime.now(), pageable
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

        meeting.update(
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.location(),
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
