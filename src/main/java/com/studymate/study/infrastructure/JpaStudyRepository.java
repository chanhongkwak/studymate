package com.studymate.study.infrastructure;

import com.studymate.study.domain.Study;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaStudyRepository extends JpaRepository<Study, UUID> {

    Optional<Study> findByIdAndDeletedAtIsNull(UUID id);
}
