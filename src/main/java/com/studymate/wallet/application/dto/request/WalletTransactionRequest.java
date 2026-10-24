package com.studymate.wallet.application.dto.request;

import com.studymate.wallet.domain.WalletTransactionType;
import java.util.UUID;

public record WalletTransactionRequest(
        UUID walletId,
        WalletTransactionType walletTransactionType,
        Long amount,
        UUID referenceId
) {
}
