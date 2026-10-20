package com.studymate.reservation.application.dto.request;

import com.studymate.reservation.domain.BranchStatus;

public record BranchStatusUpdateRequest(
        BranchStatus status
) {
}
