package com.studymate.reservation.infrastructure;

import com.studymate.reservation.domain.Branch;
import com.studymate.reservation.domain.BranchRepository;
import com.studymate.reservation.domain.BranchStatus;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BranchRepositoryImpl implements BranchRepository {
    private final BranchJpaRepository branchJpaRepository;

    @Override
    public Branch save(Branch branch) {
        return branchJpaRepository.save(branch);
    }

    @Override
    public Optional<Branch> findById(UUID id) {
        return branchJpaRepository.findById(id);
    }

    @Override
    public Page<Branch> findAllByStatus(BranchStatus status, Pageable pageable) {
        return branchJpaRepository.findAllByStatus(
                status, pageable);
    }

    @Override
    public Page<Branch> findAll(Pageable pageable) {
        return branchJpaRepository.findAll(pageable);
    }
}
