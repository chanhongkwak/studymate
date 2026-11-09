package com.studymate.reservation.application.dto.response;

import com.studymate.reservation.domain.reservation.AttendanceAction;
import java.time.LocalDateTime;

public record ReservationQrScanResponse(
        String token,
        AttendanceAction action,
        LocalDateTime expiresAt,
        LocalDateTime checkedOutAt
) {
}
