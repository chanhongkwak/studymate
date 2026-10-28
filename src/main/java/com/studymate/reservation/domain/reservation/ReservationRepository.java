package com.studymate.reservation.domain.reservation;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findByMeetingId(UUID meetingId);

    Optional<Reservation> findById(UUID reservationId);

    boolean existsOverlappingReservation(
            UUID roomId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    void flush();

    Page<Reservation> findAllByStudyId(UUID studyId, Pageable pageable);
}
