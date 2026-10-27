package com.studymate.wallet.infrastructure;

import com.studymate.wallet.domain.Wallet;
import com.studymate.wallet.domain.WalletRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {
    private final WalletJpaRepository walletJpaRepository;

    @Override
    public Wallet save(Wallet wallet) {
        return walletJpaRepository.save(wallet);
    }

    @Override
    public boolean existsByMemberId(UUID memberId) {
        return walletJpaRepository.existsByMemberId(memberId);
    }

    @Override
    public Optional<Wallet> findByMemberId(UUID memberId) {
        return walletJpaRepository.findByMemberId(memberId);
    }

    @Override
    public Optional<Wallet> findByIdForUpdate(UUID walletId) {
        return walletJpaRepository.findByIdForUpdate(walletId);
    }

    @Override
    public Optional<UUID> findIdByMemberId(UUID memberId) {
        return walletJpaRepository.findIdByMemberId(memberId);
    }
}
