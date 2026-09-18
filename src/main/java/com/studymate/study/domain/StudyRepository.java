package com.studymate.study.domain;

import java.util.Optional;
import java.util.UUID;

public interface StudyRepository {

    Study save(Study study);

    Optional<Study> findByIdAndDeletedAtIsNull(UUID id);
}
