package com.studymate.reservation.application.dto.request;

public record RoomUpdateRequest(
        String name,
        Integer capacity,
        Long hourlyRate
) {
}
