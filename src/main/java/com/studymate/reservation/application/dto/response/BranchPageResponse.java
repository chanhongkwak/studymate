package com.studymate.reservation.application.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record BranchPageResponse(
        List<BranchResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static BranchPageResponse from(
            Page<BranchResponse> result
    ) {
        return new BranchPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

}
