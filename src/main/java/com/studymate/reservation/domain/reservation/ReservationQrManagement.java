package com.studymate.reservation.domain.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "reservation_qr_management")
public class ReservationQrManagement {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID reservationId;

    private UUID delegateParticipantId;

    private UUID operatorParticipantId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;

    public ReservationQrManagement(
            UUID reservationId,
            UUID createdBy
    ) {
        if (reservationId == null) {
            throw new IllegalArgumentException("예약ID는 필수입니다.");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("생성자ID는 필수입니다.");
        }

        this.reservationId = reservationId;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.createdAt = now;
        this.createdBy = createdBy;
        this.updatedAt = now;
        this.updatedBy = createdBy;
    }

    public void changeDelegate(UUID delegateParticipantId, UUID updatedBy) {
        if (delegateParticipantId == null) {
            throw new IllegalArgumentException("대리인ID는 필수입니다.");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }
        if (delegateParticipantId.equals(this.delegateParticipantId)) {
            throw new IllegalArgumentException("해당 회원은 현재 대리인입니다.");
        }

        this.delegateParticipantId = delegateParticipantId;
        this.updatedAt = LocalDateTime.now(KOREA_ZONE);
        this.updatedBy = updatedBy;
    }

    public void changeOperator(UUID operatorParticipantId, UUID updatedBy) {
        if (operatorParticipantId == null) {
            throw new IllegalArgumentException("다음 QR담당자ID는 필수입니다.");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }
        if (operatorParticipantId.equals(this.operatorParticipantId)) {
            throw new IllegalArgumentException("해당 회원은 현재 QR담당자입니다.");
        }

        this.operatorParticipantId = operatorParticipantId;
        this.updatedAt = LocalDateTime.now(KOREA_ZONE);
        this.updatedBy = updatedBy;
    }
}
