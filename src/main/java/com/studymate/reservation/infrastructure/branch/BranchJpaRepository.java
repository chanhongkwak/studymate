package com.studymate.reservation.infrastructure.branch;

import com.studymate.reservation.domain.branch.Branch;
import com.studymate.reservation.domain.branch.BranchStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchJpaRepository extends JpaRepository<Branch, UUID> {
    Optional<Branch> findById(UUID id);

    Page<Branch> findAllByStatus(
            BranchStatus status,
            Pageable pageable
    );

    Page<Branch> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Branch b where b.id = :branchId")
    Optional<Branch> findByIdForUpdate(@Param("branchId") UUID branchId);
}
