package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.ReservationQrManagement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationQrManagementJpaRepository extends JpaRepository<ReservationQrManagement, UUID> {
    Optional<ReservationQrManagement> findByReservationId(UUID reservationId);
}
