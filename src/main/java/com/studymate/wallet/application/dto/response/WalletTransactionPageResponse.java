package com.studymate.wallet.application.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record WalletTransactionPageResponse(
        List<WalletTransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static WalletTransactionPageResponse from(
            Page<WalletTransactionResponse> transactions
    ){
        return new WalletTransactionPageResponse(
                transactions.getContent(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages()
        );
    }

}
