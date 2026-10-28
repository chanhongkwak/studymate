package com.studymate.reservation.domain.reservation;

import java.util.List;
import java.util.UUID;

public interface ReservationParticipantRepository {
    List<ReservationParticipant> findAllByReservationId(UUID reservationId);

    ReservationParticipant save(ReservationParticipant participant);
}
