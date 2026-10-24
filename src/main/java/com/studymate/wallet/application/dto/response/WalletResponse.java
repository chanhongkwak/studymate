package com.studymate.wallet.application.dto.response;

import com.studymate.wallet.domain.Wallet;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID memberId,
        long balance
) {
    public static WalletResponse from(
            Wallet wallet
    ){
        return new WalletResponse(
                wallet.getId(),
                wallet.getMemberId(),
                wallet.getBalance()
        );
    }
}
