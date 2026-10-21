package com.studymate.reservation.application.service;

import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberRole;
import com.studymate.member.domain.MemberStatus;
import com.studymate.reservation.application.dto.request.BranchRequest;
import com.studymate.reservation.application.dto.response.BranchResponse;
import com.studymate.reservation.domain.branch.Branch;
import com.studymate.reservation.domain.branch.BranchRepository;
import com.studymate.reservation.domain.branch.BranchStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchRepository branchRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public BranchResponse create(BranchRequest request, UUID createdBy) {
        if (createdBy == null) {
            throw new IllegalArgumentException("생성자ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(createdBy)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 지점을 관리할 수 있습니다.");
        }
        if (member.getRole() != MemberRole.MASTER) {
            throw new ForbiddenException("운영자만 생성 할 수 있습니다.");
        }

        Branch branch = new Branch(
                request.name(), request.address(), createdBy
        );

        branchRepository.save(branch);

        return BranchResponse.from(branch);
    }

    @Transactional
    public BranchResponse update(BranchRequest request, UUID branchId, UUID updatedBy) {

        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(updatedBy)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 지점을 관리할 수 있습니다.");
        }
        if (member.getRole() != MemberRole.MASTER) {
            throw new ForbiddenException("운영자만 수정 할 수 있습니다.");
        }

        if (branchId == null) {
            throw new IllegalArgumentException("지점ID는 필수입니다.");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("해당 지점을 찾을 수 없습니다."));

        branch.update(
                request.name(), request.address(), updatedBy
        );

        return BranchResponse.from(branch);
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranch(
            UUID branchId
    ) {
        if (branchId == null) {
            throw new IllegalArgumentException("지점ID는 필수입니다.");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("해당 지점을 찾을 수 없습니다."));

        return BranchResponse.from(branch);
    }

    @Transactional(readOnly = true)
    public Page<BranchResponse> getBranches(
            int page, int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "페이지 크기는 1 이상 100 이하여야 합니다."
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")
                )
        );

        Page<Branch> branches = branchRepository.findAllByStatus(BranchStatus.ACTIVE, pageable);

        return branches.map(BranchResponse::from);
    }

    @Transactional
    public BranchResponse changeStatus(
            UUID branchId,
            BranchStatus status,
            UUID updatedBy
    ) {
        if (updatedBy == null) {
            throw new IllegalArgumentException("상태 수정자ID는 필수입니다.");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("지점ID는 필수입니다.");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("해당 지점을 찾을 수 없습니다."));
        Member member = memberRepository.findByIdAndDeletedAtIsNull(updatedBy)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원이 아닙니다.");
        }
        if (member.getRole() != MemberRole.MASTER) {
            throw new ForbiddenException("마스터 권한이 아닙니다.");
        }
        branch.changeStatus(status, updatedBy);

        return BranchResponse.from(branch);
    }

    @Transactional(readOnly = true)
    public Page<BranchResponse> getBranchesForAdmin(
            UUID memberId,
            BranchStatus status,
            int page,
            int size
    ) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1이상 100 이하여야 합니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 조회할 수 있습니다.");
        }

        if (member.getRole() != MemberRole.MASTER) {
            throw new ForbiddenException("관리자만 조회 할 수 있습니다.");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")
                )
        );

        Page<Branch> branches;

        if (status == null) {
            branches = branchRepository.findAll(pageable);
        } else {
            branches = branchRepository.findAllByStatus(status, pageable);
        }

        return branches.map(BranchResponse::from);
    }
}
