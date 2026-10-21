package com.studymate.reservation.application.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record RoomPageResponse(
        List<RoomResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static RoomPageResponse from(
            Page<RoomResponse> result
    ) {
        return new RoomPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
