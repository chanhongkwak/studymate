package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.ReservationQrManagement;
import com.studymate.reservation.domain.reservation.ReservationQrManagementRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationQrManagementRepositoryImpl implements ReservationQrManagementRepository {
    private final ReservationQrManagementJpaRepository reservationQrManagementJpaRepository;

    @Override
    public Optional<ReservationQrManagement> findByReservationId(UUID reservationId) {
        return reservationQrManagementJpaRepository.findByReservationId(reservationId);
    }

    @Override
    public ReservationQrManagement save(ReservationQrManagement management) {
        return reservationQrManagementJpaRepository.save(management);
    }
}
