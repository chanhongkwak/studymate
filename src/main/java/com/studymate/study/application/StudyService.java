package com.studymate.study.application;

import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.study.application.dto.request.StudyCreateRequest;
import com.studymate.study.application.dto.request.StudyStatusUpdatedRequest;
import com.studymate.study.application.dto.request.StudyUpdateRequest;
import com.studymate.study.application.dto.response.StudyResponse;
import com.studymate.study.domain.Study;
import com.studymate.study.domain.StudyRepository;
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
public class StudyService {

    private final StudyRepository studyRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public StudyResponse create(UUID memberId, StudyCreateRequest request) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException("활성 회원만 스터디를 생성할 수 있습니다.");
        }

        if (request.maxMembers() == null) {
            throw new IllegalArgumentException("스터디 정원은 필수입니다.");
        }

        Study study = new Study(request.title(), request.description(), member.getId(), request.maxMembers(),
                request.activityRegion());

        studyRepository.save(study);

        return StudyResponse.from(study);
    }

    @Transactional
    public StudyResponse update(UUID studyId, UUID memberId, StudyUpdateRequest request) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디 ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "활성 회원만 스터디를 수정할 수 있습니다."
            );
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new IllegalArgumentException("스터디를 찾을 수 없습니다."));

        if (!study.getLeaderMemberId().equals(memberId)) {
            throw new IllegalArgumentException("스터디장만 수정할 수 있습니다.");
        }

        study.update(request.title(), request.description(), request.maxMembers(), request.activityRegion(),
                memberId);

        return StudyResponse.from(study);
    }

    @Transactional(readOnly = true)
    public StudyResponse getStudy(UUID studyId) {
        if (studyId == null) {
            throw new IllegalArgumentException("해당 스터디가 존재하지 않습니다.");
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디가 존재하지 않습니다."));

        return StudyResponse.from(study);
    }

    @Transactional(readOnly = true)
    public Page<StudyResponse> getStudies(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "페이지 크기는 1 이상 100 이하여야 합니다."
            );
        }

        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Study> studies =
                studyRepository.findAllByDeletedAtIsNull(pageable);

        return studies.map(StudyResponse::from);
    }

    @Transactional
    public void delete(UUID studyId, UUID memberId) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디를 찾을 수 없습니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalArgumentException("활성 회원만 스터디를 삭제할 수 있습니다.");
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new IllegalArgumentException("스터디를 찾을 수 없습니다."));

        if (!study.getLeaderMemberId().equals(member.getId())) {
            throw new IllegalArgumentException("스터디장만 삭제할 수 있습니다.");
        }

        study.delete(memberId);
    }

    @Transactional
    public StudyResponse changeStatus(
            UUID studyId,
            UUID memberId,
            StudyStatusUpdatedRequest request
    ){
        if(studyId == null){
            throw new IllegalArgumentException("스터디 ID는 필수입니다.");
        }if(memberId == null){
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if(member.getStatus() != MemberStatus.ACTIVE){
            throw new IllegalArgumentException("활성 회원만 스터디 상태를 변경할 수 있습니다.");
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() ->
                        new IllegalArgumentException("스터디를 찾을 수 없습니다.")
                );

        if (!study.getLeaderMemberId().equals(memberId)) {
            throw new IllegalArgumentException(
                    "스터디장만 상태를 변경할 수 있습니다."
            );
        }

        study.changeStatus(request.status(), memberId);

        return StudyResponse.from(study);
    }
}


