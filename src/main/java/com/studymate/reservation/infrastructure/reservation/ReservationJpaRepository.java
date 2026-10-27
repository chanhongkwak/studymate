package com.studymate.reservation.infrastructure.reservation;

import com.studymate.reservation.domain.reservation.Reservation;
import com.studymate.reservation.domain.reservation.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationJpaRepository extends JpaRepository<Reservation, UUID> {
    Reservation save(Reservation reservation);

    Optional<Reservation> findByMeetingId(UUID meetingId);

    @Query("""
        select (count(r) > 0)
        from Reservation r
        where r.roomId = :roomId
          and r.reservationStatus = :status
          and r.startAt < :endAt
          and r.endAt > :startAt
        """)
    boolean existsOverlappingReservation(
            @Param("roomId") UUID roomId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("status") ReservationStatus status
    );
}
