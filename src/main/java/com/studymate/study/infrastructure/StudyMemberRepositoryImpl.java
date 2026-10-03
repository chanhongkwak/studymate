package com.studymate.study.infrastructure;

import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberRepository;
import com.studymate.study.domain.StudyMemberStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyMemberRepositoryImpl implements StudyMemberRepository {

    private final JpaStudyMemberRepository jpaStudyMemberRepository;

    @Override
    public StudyMember save(StudyMember studyMember) {
        return jpaStudyMemberRepository.save(studyMember);
    }

    @Override
    public Optional<StudyMember> findById(UUID id) {
        return jpaStudyMemberRepository.findById(id);
    }

    @Override
    public long countByStudyIdAndStatus(UUID studyId, StudyMemberStatus status) {
        return jpaStudyMemberRepository.countByStudyIdAndStatus(studyId, status);
    }

    @Override
    public boolean existsByStudyIdAndMemberIdAndStatusIn(UUID studyId, UUID memberId,
                                                         Collection<StudyMemberStatus> statuses) {
        return jpaStudyMemberRepository.existsByStudyIdAndMemberIdAndStatusIn(studyId, memberId, statuses);
    }

    @Override
    public Optional<StudyMember> findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(UUID studyId, UUID memberId) {
        return jpaStudyMemberRepository.findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(studyId, memberId);
    }

    @Override
    public Page<StudyMember> findAllByStudyIdAndStatus(UUID studyId, StudyMemberStatus status, Pageable pageable) {
        return jpaStudyMemberRepository.findAllByStudyIdAndStatus(studyId, status, pageable);
    }

    @Override
    public List<StudyMember> findAllByStudyIdAndStatus(UUID studyId, StudyMemberStatus status) {
        return jpaStudyMemberRepository.findAllByStudyIdAndStatus(studyId, status);
    }
}
