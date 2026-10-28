package com.studymate.reservation.application.dto.response;

import com.studymate.reservation.domain.reservation.Reservation;
import com.studymate.reservation.domain.reservation.ReservationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationSummaryResponse(
        UUID id,
        UUID meetingId,
        UUID roomId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        long totalAmount,
        ReservationStatus reservationStatus
) {
    public static ReservationSummaryResponse from(Reservation reservation){
        return new ReservationSummaryResponse(
                reservation.getId(),
                reservation.getMeetingId(),
                reservation.getRoomId(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getTotalAmount(),
                reservation.getReservationStatus()
        );
    }
}
