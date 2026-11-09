package com.studymate.reservation.domain.reservation;

import java.util.Optional;
import java.util.UUID;

public interface ReservationQrManagementRepository {
    Optional<ReservationQrManagement> findByReservationId(UUID reservationId);

    ReservationQrManagement save(ReservationQrManagement management);
}
