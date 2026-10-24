package com.studymate.wallet.infrastructure;

import com.studymate.wallet.domain.WalletTransaction;
import com.studymate.wallet.domain.WalletTransactionRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WalletTransactionRepositoryImpl implements WalletTransactionRepository {
    private final WalletTransactionJpaRepository walletTransactionJpaRepository;

    @Override
    public WalletTransaction save(WalletTransaction walletTransaction) {
        return walletTransactionJpaRepository.save(walletTransaction);
    }

    @Override
    public Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey) {
        return walletTransactionJpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Page<WalletTransaction> findAllByWalletId(UUID walletId, Pageable pageable) {
        return walletTransactionJpaRepository.findAllByWalletId(walletId, pageable);
    }
}
