package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.ReservationQrCode;
import com.studymate.reservation.domain.reservation.ReservationQrCodeRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationQrCodeRepositoryImpl implements ReservationQrCodeRepository {
    private final ReservationQrCodeJpaRepository reservationQrCodeJpaRepository;

    @Override
    public ReservationQrCode save(ReservationQrCode qrCode) {
        return reservationQrCodeJpaRepository.save(qrCode);
    }

    @Override
    public Optional<ReservationQrCode> findByTokenHash(String tokenHash) {
        return reservationQrCodeJpaRepository.findByTokenHash(tokenHash);
    }
}
