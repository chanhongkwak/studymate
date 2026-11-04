package com.studymate.reservation.domain.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "reservation_attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_attendance_participant",
                columnNames = "reservation_participant_id"
        )
)
public class ReservationAttendance {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reservation_participant_id", nullable = false, updatable = false)
    private UUID reservationParticipantId;

    @Column(nullable = false , updatable = false)
    private LocalDateTime checkedInAt;

    private LocalDateTime checkedOutAt;

    public ReservationAttendance(
            UUID reservationParticipantId,
            LocalDateTime checkedInAt
    ){
        if(reservationParticipantId == null){
            throw new IllegalArgumentException("예약 참석자 ID는 필수입니다.");
        }
        if(checkedInAt == null){
            throw new IllegalArgumentException("입실 시간은 필수입니다.");
        }
        this.reservationParticipantId = reservationParticipantId;
        this.checkedInAt = checkedInAt;
        this.checkedOutAt = null;
    }

    public void checkOut(
            LocalDateTime checkedOutAt
    ){
        if(checkedOutAt == null){
            throw new IllegalArgumentException("퇴실 시각은 필수입니다.");
        }
        if(this.checkedOutAt != null){
            return;
        }
        if(checkedOutAt.isBefore(this.checkedInAt)){
            throw new IllegalArgumentException("퇴실 시각은 입실 시각보다 빠를 수 없습니다.");
        }

        this.checkedOutAt = checkedOutAt;
    }
}
