package com.studymate.reservation.application.dto.request;

import java.util.UUID;

public record ReservationQrDelegateRequest(
        UUID delegateParticipantId
) {
}
