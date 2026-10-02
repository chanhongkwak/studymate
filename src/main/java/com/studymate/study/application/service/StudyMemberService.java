package com.studymate.study.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.study.application.dto.request.StudyMemberRequest;
import com.studymate.study.application.dto.response.StudyMemberResponse;
import com.studymate.study.domain.Study;
import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberRepository;
import com.studymate.study.domain.StudyMemberStatus;
import com.studymate.study.domain.StudyRepository;
import com.studymate.study.domain.StudyStatus;
import java.util.List;
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
public class StudyMemberService {

    private final MemberRepository memberRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional
    public StudyMemberResponse apply(UUID studyId, UUID memberId, StudyMemberRequest request) {

        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("해당 스터디를 찾을 수 없습니다."));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 가입을 신청할 수 있습니다.");
        }

        if (study.getStatus() != StudyStatus.RECRUITING) {
            throw new ConflictException("모집 중인 스터디에만 신청할 수 있습니다.");
        }

        boolean alreadyApplied = studyMemberRepository.existsByStudyIdAndMemberIdAndStatusIn(studyId, memberId,
                List.of(
                        StudyMemberStatus.ACTIVE,
                        StudyMemberStatus.PENDING_APPROVAL
                )
        );

        if (alreadyApplied) {
            throw new ConflictException("해당 스터디에 이미 신청했거나 참여 중입니다.");
        }

        StudyMember studyMember = new StudyMember(study.getId(), member.getId(), request.applicationMessage());

        studyMemberRepository.save(studyMember);

        return StudyMemberResponse.from(studyMember);
    }

    @Transactional(readOnly = true)
    public StudyMemberResponse getMyLatestApplication(UUID studyId, UUID memberId) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() ->
                        new NotFoundException("해당 회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 신청 내역을 조회할 수 있습니다.");
        }

        studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() ->
                        new NotFoundException("해당 스터디를 찾을 수 없습니다."));

        StudyMember studyMember = studyMemberRepository.findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(studyId,
                        memberId)
                .orElseThrow(() -> new NotFoundException("가입 신청 내역을 찾을 수 없습니다."));

        return StudyMemberResponse.from(studyMember);
    }

    @Transactional(readOnly = true)
    public Page<StudyMemberResponse> getPendingApplications(
            UUID studyId,
            UUID memberId,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() ->
                        new NotFoundException("해당 회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 신청 목록을 조회할 수 있습니다.");
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() ->
                        new NotFoundException("해당 스터디를 찾을 수 없습니다."));

        if (!study.getLeaderMemberId().equals(memberId)) {
            throw new ForbiddenException("스터디장만 가입 신청 목록을 조회할 수 있습니다.");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<StudyMember> applications =
                studyMemberRepository.findAllByStudyIdAndStatus(
                        studyId,
                        StudyMemberStatus.PENDING_APPROVAL,
                        pageable
                );

        return applications.map(StudyMemberResponse::from);
    }

    @Transactional
    public StudyMemberResponse approve(UUID studyId,
                                       UUID studyMemberId,
                                       UUID memberId) {
        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("해당 스터디를 찾을 수 없습니다."));

        if (study.getStatus() == StudyStatus.ENDED) {
            throw new ConflictException("종료된 스터디에서는 승인할 수 없습니다.");
        }

        StudyMember studyMember = studyMemberRepository.findById(studyMemberId)
                .orElseThrow(() -> new NotFoundException("가입 신청 내역이 없습니다."));

        if (!studyMember.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 가입 신청을 찾을 수 없습니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (!study.getLeaderMemberId().equals(memberId)) {
            throw new ForbiddenException("승인은 그룹장만 가능합니다.");
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원이 아닙니다.");
        }

        if (studyMemberRepository.countByStudyIdAndStatus(studyId, StudyMemberStatus.ACTIVE) >= study.getMaxMembers()) {
            throw new ConflictException("정원이 다 찼습니다.");
        }

        Member applicant = memberRepository
                .findByIdAndDeletedAtIsNull(studyMember.getMemberId())
                .orElseThrow(() -> new NotFoundException("신청자를 찾을 수 없습니다."));

        if (applicant.getStatus() != MemberStatus.ACTIVE) {
            throw new ConflictException("활성 상태인 신청자만 승인할 수 있습니다.");
        }

        studyMember.approve(memberId);

        return StudyMemberResponse.from(studyMember);
    }

    @Transactional
    public StudyMemberResponse reject(UUID studyId, UUID studyMemberId, UUID memberId) {
        Study study = studyRepository.findByIdForUpdate(studyId)
                .orElseThrow(() -> new NotFoundException("해당 스터디를 찾을 수 없습니다."));

        StudyMember studyMember = studyMemberRepository.findById(studyMemberId)
                .orElseThrow(() -> new NotFoundException("가입 신청 내역이 없습니다."));

        if (!studyMember.getStudyId().equals(studyId)) {
            throw new NotFoundException("해당 스터디의 가입 신청을 찾을 수 없습니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (!study.getLeaderMemberId().equals(memberId)) {
            throw new ForbiddenException("거절은 그룹장만 가능합니다.");
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원이 아닙니다.");
        }

        studyMember.reject(memberId);

        return StudyMemberResponse.from(studyMember);
    }


}
