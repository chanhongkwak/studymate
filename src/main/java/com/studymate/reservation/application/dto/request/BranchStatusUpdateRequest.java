package com.studymate.reservation.application.dto.request;

import com.studymate.reservation.domain.branch.BranchStatus;

public record BranchStatusUpdateRequest(
        BranchStatus status
) {
}
