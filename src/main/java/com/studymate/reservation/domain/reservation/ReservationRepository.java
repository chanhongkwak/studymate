package com.studymate.reservation.domain.reservation;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findByMeetingId(UUID meetingId);

    boolean existsOverlappingReservation(
            UUID roomId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    void flush();
}
