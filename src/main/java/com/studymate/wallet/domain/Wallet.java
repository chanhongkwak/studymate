package com.studymate.wallet.domain;

import com.studymate.global.exception.ConflictException;
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
@Table(name = "wallet")
public class Wallet {
    private final static ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID memberId;

    @Column(nullable = false)
    private long balance;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(updatable = false)
    private UUID createdBy;

    private UUID updatedBy;

    public Wallet(
            UUID memberId,
            UUID createdBy
    ) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }
        this.memberId = memberId;
        this.balance = 0L;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public void credit(
            long amount,
            UUID updatedBy
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 금액은 1 MC 이상이어야 합니다.");
        }

        if (this.balance > Long.MAX_VALUE - amount) {
            throw new ConflictException("보유 가능한 MC 범위를 초과합니다.");
        }
        this.balance += amount;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    public void debit(
            long amount,
            UUID updatedBy
    ) {
        if (amount > this.balance) {
            throw new ConflictException("잔액이 부족합니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 1 MC 이상이어야 합니다.");
        }

        this.balance -= amount;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }
}
