package com.studymate.reservation.presentation;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.reservation.application.dto.request.BranchRequest;
import com.studymate.reservation.application.dto.request.BranchStatusUpdateRequest;
import com.studymate.reservation.application.dto.response.BranchPageResponse;
import com.studymate.reservation.application.dto.response.BranchResponse;
import com.studymate.reservation.application.service.BranchService;
import com.studymate.reservation.domain.BranchStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branches")
public class BranchController {
    private final BranchService branchService;

    @PostMapping
    public ResponseEntity<BranchResponse> create(
            @RequestBody BranchRequest request,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        BranchResponse response = branchService.create(
                request, principal.getMemberId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{branchId}")
    public ResponseEntity<BranchResponse> update(
            @PathVariable UUID branchId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody BranchRequest request
    ) {
        BranchResponse response = branchService.update(
                request,
                branchId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{branchId}")
    public ResponseEntity<BranchResponse> getBranch(
            @PathVariable("branchId") UUID branchId
    ) {
        BranchResponse response = branchService.getBranch(branchId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<BranchPageResponse> getBranches(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Page<BranchResponse> result = branchService.getBranches(page, size);

        return ResponseEntity.ok(BranchPageResponse.from(result));
    }

    @PatchMapping("/{branchId}/status")
    public ResponseEntity<BranchResponse> changeStatus(
            @PathVariable UUID branchId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody BranchStatusUpdateRequest request
    ) {
        BranchResponse response = branchService.changeStatus(
                branchId, request.status(), principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    public ResponseEntity<BranchPageResponse> getBranchesForAdmin(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false) BranchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<BranchResponse> result = branchService.getBranchesForAdmin(
                principal.getMemberId(),
                status,
                page,
                size
        );

        return ResponseEntity.ok(BranchPageResponse.from(result));
    }
}
