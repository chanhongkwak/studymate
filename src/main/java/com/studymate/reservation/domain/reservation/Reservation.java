package com.studymate.reservation.domain.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "reservation")
public class Reservation {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID meetingId;

    @Column(nullable = false, updatable = false)
    private UUID roomId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime endAt;

    @Column(nullable = false, updatable = false)
    private long hourlyRate;

    @Column(nullable = false, updatable = false)
    private long totalAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;

    public Reservation(
            UUID meetingId,
            UUID roomId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            long hourlyRate,
            UUID createdBy
    ) {
        if (meetingId == null) {
            throw new IllegalArgumentException("모임ID는 필수입니다.");
        }
        if (roomId == null) {
            throw new IllegalArgumentException("방ID는 필수입니다.");
        }
        if (startAt == null) {
            throw new IllegalArgumentException("시작시간은 필수입니다.");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("종료시간은 필수입니다.");
        }
        if (!startAt.toLocalDate().equals(endAt.toLocalDate())) {
            throw new IllegalArgumentException("예약 시작과 종료는 같은 날짜여야 합니다.");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("종료 시각은 시작 시각보다 이후여야 합니다.");
        }
        if (startAt.toLocalTime().isBefore(LocalTime.of(9, 0))
                || endAt.toLocalTime().isAfter(LocalTime.of(23, 0))) {
            throw new IllegalArgumentException("예약은 09:00부터 23:00 사이에 이용해야 합니다.");
        }
        if (startAt.getMinute() != 0
                || startAt.getSecond() != 0
                || startAt.getNano() != 0) {
            throw new IllegalArgumentException("예약 시작 시각은 정각이어야 합니다.");
        }
        if (endAt.getMinute() != 0
                || endAt.getSecond() != 0
                || endAt.getNano() != 0) {
            throw new IllegalArgumentException("예약 종료 시각은 정각이어야 합니다.");
        }
        if (hourlyRate < 1) {
            throw new IllegalArgumentException("시간당 MC 금액은 1이상이어야 합니다.");
        }

        if (createdBy == null) {

            throw new IllegalArgumentException("생성자ID는 필수입니다.");
        }

        this.meetingId = meetingId;
        this.roomId = roomId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.hourlyRate = hourlyRate;

        long hours = Duration.between(startAt, endAt).toHours();
        this.totalAmount = Math.multiplyExact(hourlyRate, hours);

        this.reservationStatus = ReservationStatus.CONFIRMED;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.createdAt = now;
        this.createdBy = createdBy;
        this.updatedAt = now;
        this.updatedBy = createdBy;
    }

    public void cancel(
            UUID updatedBy
    ) {
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }
        if (this.reservationStatus == ReservationStatus.CANCELED) {
            throw new IllegalArgumentException("이미 취소된 예약입니다.");
        }

        this.reservationStatus = ReservationStatus.CANCELED;
        this.updatedAt = LocalDateTime.now(KOREA_ZONE);
        this.updatedBy = updatedBy;
    }
}
