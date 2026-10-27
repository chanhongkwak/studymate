package com.studymate.reservation.application.dto.response;

import com.studymate.reservation.domain.reservation.Reservation;
import com.studymate.reservation.domain.reservation.ReservationParticipant;
import com.studymate.reservation.domain.reservation.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID meetingId,
        UUID roomId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        long hourlyRate,
        long totalAmount,
        ReservationStatus reservationStatus,
        List<ReservationParticipantResponse> participants
) {
    public static ReservationResponse from(
            Reservation reservation,
            List<ReservationParticipant> participants
    ) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getMeetingId(),
                reservation.getRoomId(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getHourlyRate(),
                reservation.getTotalAmount(),
                reservation.getReservationStatus(),
                participants.stream()
                        .map(ReservationParticipantResponse::from)
                        .toList()
        );
    }
}
