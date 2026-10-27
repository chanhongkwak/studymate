package com.studymate.reservation.domain.branch;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BranchRepository {
    Branch save(Branch branch);

    Optional<Branch> findById(UUID id);

    Page<Branch> findAllByStatus(
            BranchStatus status,
            Pageable pageable
    );

    Page<Branch> findAll(Pageable pageable);

    Optional<Branch> findByIdForUpdate(UUID branchId);
}
