package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.Reservation;
import com.studymate.reservation.domain.reservation.ReservationRepository;
import com.studymate.reservation.domain.reservation.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {
    private final ReservationJpaRepository reservationJpaRepository;

    @Override
    public Reservation save(Reservation reservation) {
        return reservationJpaRepository.save(reservation);
    }

    @Override
    public Optional<Reservation> findByMeetingId(UUID meetingId) {
        return reservationJpaRepository.findByMeetingId(meetingId);
    }

    @Override
    public boolean existsOverlappingReservation(
            UUID roomId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return reservationJpaRepository.existsOverlappingReservation(
                roomId,
                startAt,
                endAt,
                ReservationStatus.CONFIRMED
        );
    }

    @Override
    public void flush() {
        reservationJpaRepository.flush();
    }
}
