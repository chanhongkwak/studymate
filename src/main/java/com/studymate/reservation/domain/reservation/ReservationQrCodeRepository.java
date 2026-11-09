package com.studymate.reservation.domain.reservation;

import java.util.Optional;

public interface ReservationQrCodeRepository {
    ReservationQrCode save(ReservationQrCode qrCode);

    Optional<ReservationQrCode> findByTokenHash(String tokenHash);
}
