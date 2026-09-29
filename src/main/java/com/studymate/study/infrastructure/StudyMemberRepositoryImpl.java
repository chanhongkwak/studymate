package com.studymate.study.infrastructure;

import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberRepository;
import com.studymate.study.domain.StudyMemberStatus;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
}
