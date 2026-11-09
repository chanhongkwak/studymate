package com.studymate.reservation.domain.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "reservation_attendance_grant")
public class ReservationAttendanceGrant {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID reservationParticipantId;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AttendanceAction action;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime usedAt;

    public ReservationAttendanceGrant(
            UUID reservationParticipantId,
            String tokenHash,
            AttendanceAction action
    ) {
        if (reservationParticipantId == null) {
            throw new IllegalArgumentException("예약 참석자 ID는 필수입니다.");
        }

        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("개인 권한 토큰의 해시값은 필수입니다.");
        }

        if (!tokenHash.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("개인 권한 토큰의 해시값은 필수입니다.");
        }

        if (action == null) {
            throw new IllegalArgumentException("입실 또는 퇴실 용도는 필수입니다.");
        }

        this.reservationParticipantId = reservationParticipantId;
        this.tokenHash = tokenHash;
        this.action = action;
        this.issuedAt = LocalDateTime.now(KOREA_ZONE);
        this.expiresAt = this.issuedAt.plusSeconds(60);
    }

    public void use(LocalDateTime usedAt) {
        if (usedAt == null) {
            throw new IllegalArgumentException("사용 시각은 필수입니다.");
        }

        if (this.usedAt != null) {
            throw new IllegalArgumentException("이미 사용한 권한입니다.");
        }

        if (usedAt.isBefore(this.issuedAt)) {
            throw new IllegalArgumentException("사용 시각은 발급 시간보다 빠를 수 없습니다.");
        }

        if (!usedAt.isBefore(this.expiresAt)) {
            throw new IllegalArgumentException("만료된 권한입니다.");
        }

        this.usedAt = usedAt;
    }
}
