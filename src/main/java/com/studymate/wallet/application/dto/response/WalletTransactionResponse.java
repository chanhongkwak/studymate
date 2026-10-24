package com.studymate.wallet.application.dto.response;

import com.studymate.wallet.domain.WalletTransaction;
import com.studymate.wallet.domain.WalletTransactionType;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        UUID walletId,
        WalletTransactionType walletTransactionType,
        long amount,
        long balanceAfter,
        UUID referenceId,
        LocalDateTime createdAt
) {
    public static WalletTransactionResponse from(
            WalletTransaction walletTransaction
    ) {
        return new WalletTransactionResponse(
                walletTransaction.getId(),
                walletTransaction.getWalletId(),
                walletTransaction.getWalletTransactionType(),
                walletTransaction.getAmount(),
                walletTransaction.getBalanceAfter(),
                walletTransaction.getReferenceId(),
                walletTransaction.getCreatedAt()
        );
    }
}
