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
        name = "reservation_participant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_participant_reservation_member",
                columnNames = {"reservation_id", "member_id"}
        )
)
public class ReservationParticipant {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    public ReservationParticipant(
            UUID reservationId,
            UUID memberId,
            long amount,
            UUID createdBy
    ){
        if(reservationId == null){
            throw new IllegalArgumentException("예약ID는 필수입니다.");
        }
        if(memberId == null){
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }
        if(amount < 0){
            throw new IllegalArgumentException("MC값은 음수가 될 수 없습니다.");
        }
        if(createdBy == null){
            throw new IllegalArgumentException("생성자ID는 필수입니다.");
        }

        this.reservationId = reservationId;
        this.memberId = memberId;
        this.amount = amount;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.createdAt = now;
        this.createdBy = createdBy;
    }
}
