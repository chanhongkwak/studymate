package com.studymate.reservation.application.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationCreateRequest(
        UUID meetingId,
        UUID roomId
) {
}
