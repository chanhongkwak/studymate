package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.ReservationAttendance;
import com.studymate.reservation.domain.reservation.ReservationAttendanceRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationAttendanceRepositoryImpl implements ReservationAttendanceRepository {
    private final ReservationAttendanceJpaRepository reservationAttendanceJpaRepository;

    @Override
    public ReservationAttendance save(ReservationAttendance attendance) {
        return reservationAttendanceJpaRepository.save(attendance);
    }

    @Override
    public Optional<ReservationAttendance> findByReservationParticipantId(UUID reservationParticipantId) {
        return reservationAttendanceJpaRepository.findByReservationParticipantId(reservationParticipantId);
    }
}
