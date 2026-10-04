package com.studymate.study.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudyMemberRepository {

    StudyMember save(StudyMember studyMember);

    Optional<StudyMember> findById(UUID id);

    long countByStudyIdAndStatus(UUID studyId, StudyMemberStatus status);

    boolean existsByStudyIdAndMemberIdAndStatusIn(
            UUID studyId,
            UUID memberId,
            Collection<StudyMemberStatus> statuses
    );

    Optional<StudyMember> findFirstByStudyIdAndMemberIdOrderByCreatedAtDescIdDesc(
            UUID studyId,
            UUID memberId
    );

    Page<StudyMember> findAllByStudyIdAndStatus(
            UUID studyId,
            StudyMemberStatus status,
            Pageable pageable
    );

    List<StudyMember> findAllByStudyIdAndStatus(
            UUID studyId,
            StudyMemberStatus status
    );
}
