package com.studymate.reservation.application.dto.request;

import java.util.UUID;

public record RoomCreateRequest(
        UUID branchId,
        String name,
        Integer capacity,
        Long hourlyRate
) {
}
