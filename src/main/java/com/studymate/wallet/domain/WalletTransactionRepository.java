package com.studymate.wallet.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletTransactionRepository {
    WalletTransaction save(WalletTransaction walletTransaction);

    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    Page<WalletTransaction> findAllByWalletId(UUID walletId, Pageable pageable);
}
