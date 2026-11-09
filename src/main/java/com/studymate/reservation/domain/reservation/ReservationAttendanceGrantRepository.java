package com.studymate.reservation.domain.reservation;

import java.util.Optional;

public interface ReservationAttendanceGrantRepository {

    ReservationAttendanceGrant save(ReservationAttendanceGrant grant);

    Optional<ReservationAttendanceGrant> findByTokenHash(String tokenHash);
}
