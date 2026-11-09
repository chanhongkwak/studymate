package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.ReservationQrCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationQrCodeJpaRepository extends JpaRepository<ReservationQrCode , UUID> {

    Optional<ReservationQrCode> findByTokenHash(String tokenHash);

}
