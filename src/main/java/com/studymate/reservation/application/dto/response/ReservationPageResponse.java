package com.studymate.reservation.application.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record ReservationPageResponse(
        List<ReservationSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ReservationPageResponse from(
            Page<ReservationSummaryResponse> result
    ){
        return new ReservationPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
