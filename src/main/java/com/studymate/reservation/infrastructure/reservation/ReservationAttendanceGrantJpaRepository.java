package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.ReservationAttendanceGrant;
import com.studymate.reservation.domain.reservation.ReservationAttendanceGrantRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationAttendanceGrantJpaRepository extends JpaRepository<ReservationAttendanceGrant , UUID> {
    Optional<ReservationAttendanceGrant> findByTokenHash(String tokenHash);
}
