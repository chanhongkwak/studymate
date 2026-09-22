package com.studymate.study.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudyRepository {

    Study save(Study study);

    Optional<Study> findByIdAndDeletedAtIsNull(UUID id);

    Page<Study> findAllByDeletedAtIsNull(Pageable pageable);
}
