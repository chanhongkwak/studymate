package com.studymate.wallet;

import com.studymate.global.domain.ActivityRegion;
import com.studymate.member.domain.Member;
import com.studymate.wallet.application.dto.request.WalletTransactionRequest;
import com.studymate.wallet.application.service.WalletTransactionService;
import com.studymate.wallet.domain.Wallet;
import com.studymate.wallet.domain.WalletTransaction;
import com.studymate.wallet.domain.WalletTransactionType;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wallet-charge-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=wallet-test-only-secret-012345678901234567890123456789",
        "jwt.access-token-expiration-seconds=3600"
})
@Transactional
class WalletTransactionServiceTest {
    @Autowired
    private WalletTransactionService service;

    @Autowired
    private EntityManager entityManager;

    @Test
    void chargeAddsBalanceAndSavesTransaction() {
        Member member = new Member("charge-test@example.com", "test-encoded-password",
                "charge-test", ActivityRegion.SEOUL_GANGNAM);
        entityManager.persist(member);
        Wallet wallet = new Wallet(member.getId(), member.getId());
        entityManager.persist(wallet);
        entityManager.flush();
        UUID walletId = wallet.getId();
        UUID paymentId = UUID.randomUUID();

        var response = service.create(new WalletTransactionRequest(
                walletId, WalletTransactionType.PAYMENT_CHARGE, 100L, paymentId
        ), member.getId());

        // Flush and clear so assertions read stored data instead of managed objects.
        entityManager.flush();
        entityManager.clear();
        Wallet storedWallet = entityManager.find(Wallet.class, walletId);
        WalletTransaction stored = entityManager.find(WalletTransaction.class, response.id());
        assertNotNull(stored);
        assertEquals(100L, storedWallet.getBalance());
        assertEquals(100L, stored.getAmount());
        assertEquals(100L, stored.getBalanceAfter());
        assertEquals(walletId, stored.getWalletId());
        assertEquals(paymentId, stored.getReferenceId());
        assertEquals(WalletTransactionType.PAYMENT_CHARGE, stored.getWalletTransactionType());
        assertEquals("PAYMENT_CHARGE:" + paymentId + ":" + walletId, stored.getIdempotencyKey());
        assertEquals(member.getId(), stored.getCreatedBy());
        assertNotNull(stored.getCreatedAt());
        long count = entityManager.createQuery(
                "select count(t) from WalletTransaction t where t.walletId = :walletId", Long.class
        ).setParameter("walletId", walletId).getSingleResult();
        assertEquals(1L, count);
    }
}
