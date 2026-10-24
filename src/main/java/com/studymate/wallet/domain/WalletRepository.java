package com.studymate.wallet.domain;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Wallet save(Wallet wallet);

    boolean existsByMemberId(UUID memberId);

    Optional<Wallet> findByMemberId(UUID memberId);

    Optional<Wallet> findByIdForUpdate(UUID walletId);
}
