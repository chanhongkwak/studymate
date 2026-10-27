package com.studymate.wallet.infrastructure;

import com.studymate.wallet.domain.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletJpaRepository extends JpaRepository<Wallet, UUID> {
    boolean existsByMemberId(UUID memberId);

    Optional<Wallet> findByMemberId(UUID memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :walletId")
    Optional<Wallet> findByIdForUpdate(@Param("walletId") UUID walletId);

    @Query("select w.id from Wallet w where w.memberId = :memberId")
    Optional<UUID> findIdByMemberId(@Param("memberId") UUID memberId);
}
