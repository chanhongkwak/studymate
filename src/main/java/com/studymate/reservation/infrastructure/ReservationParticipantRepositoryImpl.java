package com.studymate.reservation.infrastructure;

import com.studymate.reservation.domain.reservation.ReservationParticipantRepository;
import com.studymate.reservation.domain.reservation.ReservationParticipant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationParticipantRepositoryImpl implements ReservationParticipantRepository {
    private final ReservationParticipantJpaRepository reservationParticipantJpaRepository;

    @Override
    public List<ReservationParticipant> findAllByReservationId(UUID reservationId) {
        return reservationParticipantJpaRepository.findAllByReservationId(reservationId);
    }

    @Override
    public ReservationParticipant save(ReservationParticipant participant) {
        return reservationParticipantJpaRepository.save(participant);
    }
}
