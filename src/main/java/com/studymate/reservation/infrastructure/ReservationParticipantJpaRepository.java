package com.studymate.reservation.infrastructure;

import com.studymate.reservation.domain.reservation.ReservationParticipant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationParticipantJpaRepository extends JpaRepository<ReservationParticipant, UUID> {
    List<ReservationParticipant> findAllByReservationId(UUID reservationId);

}
