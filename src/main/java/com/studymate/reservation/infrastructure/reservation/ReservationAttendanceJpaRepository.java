package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.ReservationAttendance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationAttendanceJpaRepository extends JpaRepository<ReservationAttendance , UUID> {

    Optional<ReservationAttendance> findByReservationParticipantId(
            UUID reservationParticipantId
    );
}
