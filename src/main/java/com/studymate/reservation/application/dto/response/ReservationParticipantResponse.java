package com.studymate.reservation.application.dto.response;

import com.studymate.reservation.domain.reservation.ReservationParticipant;
import java.util.UUID;

public record ReservationParticipantResponse(
        UUID memberId,
        long amount
) {
    public static ReservationParticipantResponse from(
            ReservationParticipant participant
    ){
        return new ReservationParticipantResponse(
                participant.getMemberId(),
                participant.getAmount()
        );
    }
}
