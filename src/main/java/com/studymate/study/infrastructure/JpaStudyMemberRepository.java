package com.studymate.study.infrastructure;

import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaStudyMemberRepository extends JpaRepository<StudyMember, UUID> {

    long countByStudyIdAndStatus(UUID studyId, StudyMemberStatus status);
}
