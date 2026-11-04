package com.studymate.reservation.domain.reservation;

import java.util.Optional;
import java.util.UUID;

public interface ReservationAttendanceRepository {
    ReservationAttendance save(ReservationAttendance attendance);

    Optional<ReservationAttendance> findByReservationParticipantId(
            UUID reservationParticipantId
    );
}
