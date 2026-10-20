package com.studymate.reservation.application.dto.response;

import com.studymate.reservation.domain.Branch;
import com.studymate.reservation.domain.BranchStatus;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        String name,
        String address,
        BranchStatus status
) {
    public static BranchResponse from(
            Branch branch
    ){
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getAddress(),
                branch.getStatus()
        );
    }
}
