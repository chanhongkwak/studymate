package com.studymate.wallet.domain;

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
@Table(name = "wallet_transaction")
public class WalletTransaction {
    private final static ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID walletId;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private WalletTransactionType walletTransactionType;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Column(nullable = false, updatable = false)
    private long balanceAfter;

    @Column(nullable = false, updatable = false)
    private UUID referenceId;

    @Column(nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(updatable = false)
    private UUID createdBy;

    public WalletTransaction(
            UUID walletId,
            WalletTransactionType walletTransactionType,
            long amount,
            long balanceAfter,
            UUID referenceId,
            String idempotencyKey,
            UUID createdBy
    ) {
        if (walletId == null) {
            throw new IllegalArgumentException("지갑ID는 필수입니다.");
        }
        if (walletTransactionType == null) {
            throw new IllegalArgumentException("거래내역 타입은 필수입니다.");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("변경 될 수량은 1이상이어야 합니다.");
        }
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("거래 직후 잔액은 0이상이어야 합니다.");
        }
        if (referenceId == null) {
            throw new IllegalArgumentException("거래 원인의 ID는 필수입니다.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("거래내역 식별값은 필수입니다.");
        }
        if (idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("거래내역 식별값은 255자를 초과할 수 없습니다.");
        }

        this.walletId = walletId;
        this.walletTransactionType = walletTransactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;

        this.createdAt = LocalDateTime.now(KOREA_ZONE);
        this.createdBy = createdBy;
    }
}
