package com.studymate.study.infrastructure;

import com.studymate.study.domain.Study;
import com.studymate.study.domain.StudyRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyRepositoryImpl implements StudyRepository {

    private final JpaStudyRepository jpaStudyRepository;

    @Override
    public Study save(Study study) {
        return jpaStudyRepository.save(study);
    }

    @Override
    public Optional<Study> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaStudyRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Study> findAllByDeletedAtIsNull(Pageable pageable) {
        return jpaStudyRepository.findAllByDeletedAtIsNull(pageable);
    }
}
