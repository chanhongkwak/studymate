package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.ReservationAttendanceGrant;
import com.studymate.reservation.domain.reservation.ReservationAttendanceGrantRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationAttendanceGrantRepositoryImpl implements ReservationAttendanceGrantRepository {
    private final ReservationAttendanceGrantJpaRepository reservationAttendanceGrantJpaRepository;

    @Override
    public ReservationAttendanceGrant save(ReservationAttendanceGrant grant) {
        return reservationAttendanceGrantJpaRepository.save(grant);
    }

    @Override
    public Optional<ReservationAttendanceGrant> findByTokenHash(String tokenHash) {
        return reservationAttendanceGrantJpaRepository.findByTokenHash(tokenHash);
    }
}
