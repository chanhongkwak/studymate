package com.studymate.reservation.domain;

import com.studymate.reservation.domain.reservation.ReservationParticipant;
import java.util.List;
import java.util.UUID;

public interface ReservationParticipantRepository {
    List<ReservationParticipant> findAllByReservationId(UUID reservationId);

    ReservationParticipant save(ReservationParticipant participant);
}
