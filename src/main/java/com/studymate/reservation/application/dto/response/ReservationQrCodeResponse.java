package com.studymate.reservation.application.dto.response;

import java.time.LocalDateTime;

public record ReservationQrCodeResponse(
        String token,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt
) {
}
