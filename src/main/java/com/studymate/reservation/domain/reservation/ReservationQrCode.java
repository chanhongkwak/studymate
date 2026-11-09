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
@Table(name = "reservation_qr_code")
public class ReservationQrCode {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false,updatable = false)
    private UUID reservationId;

    @Column(nullable = false,unique = true, updatable = false , length = 64)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private UUID issuedByParticipantId;

    @Column(nullable = false,updatable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false,updatable = false)
    private LocalDateTime expiredAt;

    public ReservationQrCode(
            UUID reservationId,
            String tokenHash,
            UUID issuedByParticipantId
    ){
        if(reservationId == null){
            throw new IllegalArgumentException("예약ID는 필수입니다.");
        }
        if(tokenHash == null || tokenHash.isBlank()){
            throw new IllegalArgumentException("해시값은 필수입니다.");
        }
        if(!tokenHash.matches("[0-9a-fA-F]{64}")){
            throw new IllegalArgumentException("해시값 길이는 64를 넘어 갈 수 없습니다.");
        }
        if(issuedByParticipantId == null){
            throw new IllegalArgumentException("QR담당자의 예약 참석자ID는 필수입니다.");
        }

        this.reservationId = reservationId;
        this.tokenHash = tokenHash;
        this.issuedByParticipantId = issuedByParticipantId;
        this.issuedAt = LocalDateTime.now(KOREA_ZONE);
        this.expiredAt = issuedAt.plusSeconds(60);
    }
}
