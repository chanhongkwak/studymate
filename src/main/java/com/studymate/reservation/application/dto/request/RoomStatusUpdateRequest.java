package com.studymate.reservation.application.dto.request;

import com.studymate.reservation.domain.room.RoomStatus;

public record RoomStatusUpdateRequest(
        RoomStatus status
) {
}
