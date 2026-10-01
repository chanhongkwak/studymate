package com.studymate.study.infrastructure;

import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaStudyMemberRepository extends JpaRepository<StudyMember, UUID> {

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
}
