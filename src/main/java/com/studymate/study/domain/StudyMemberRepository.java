package com.studymate.study.domain;

import java.util.Optional;
import java.util.UUID;

public interface StudyMemberRepository {

    StudyMember save(StudyMember studyMember);

    Optional<StudyMember> findById(UUID id);

    long countByStudyIdAndStatus(UUID studyId, StudyMemberStatus status);
}
