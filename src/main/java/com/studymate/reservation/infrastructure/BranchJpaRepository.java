package com.studymate.reservation.infrastructure;

import com.studymate.reservation.domain.Branch;
import com.studymate.reservation.domain.BranchStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchJpaRepository extends JpaRepository<Branch, UUID> {
    Optional<Branch> findById(UUID id);

    Page<Branch> findAllByStatus(
            BranchStatus status,
            Pageable pageable
    );

    Page<Branch> findAll(Pageable pageable);
}
