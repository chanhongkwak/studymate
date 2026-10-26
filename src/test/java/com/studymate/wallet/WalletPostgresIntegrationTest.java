package com.studymate.wallet;

import com.studymate.global.domain.ActivityRegion;
import com.studymate.global.exception.ConflictException;
import com.studymate.member.domain.Member;
import com.studymate.wallet.application.dto.request.WalletTransactionRequest;
import com.studymate.wallet.application.service.WalletTransactionService;
import com.studymate.wallet.domain.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

// Opt-in: this dedicated disposable database must never point at development data.
@EnabledIfEnvironmentVariable(named = "WALLET_PG_TEST", matches = "true")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55436/wallet_test",
    "spring.datasource.username=postgres", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=wallet-test-only-secret-012345678901234567890123456789",
    "jwt.access-token-expiration-seconds=3600"
})
class WalletPostgresIntegrationTest {
    @Autowired WalletTransactionService service;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    UUID memberId;
    UUID walletId;
    <T> T tx(Supplier<T> work) {
        return new TransactionTemplate(manager).execute(status -> work.get());
    }
    @BeforeEach void prepare() {
        tx(() -> {
            Member member = new Member(UUID.randomUUID()+"@example.com", "test-password",
                    "test", ActivityRegion.SEOUL_GANGNAM);
            em.persist(member);
            memberId = member.getId();
            Wallet wallet = new Wallet(memberId, memberId);
            em.persist(wallet);
            walletId = wallet.getId();
            return null;
        });
        call(WalletTransactionType.PAYMENT_CHARGE, 100, UUID.randomUUID());
    }
    @AfterEach void cleanup() {
        if (walletId == null) return;
        tx(() -> {
            em.createQuery("delete from WalletTransaction t where t.walletId=:id")
                    .setParameter("id", walletId).executeUpdate();
            em.createQuery("delete from Wallet w where w.id=:id")
                    .setParameter("id", walletId).executeUpdate();
            em.createQuery("delete from Member m where m.id=:id")
                    .setParameter("id", memberId).executeUpdate();
            return null;
        });
    }
    UUID call(WalletTransactionType type, long amount, UUID ref) {
        return service.create(new WalletTransactionRequest(walletId, type, amount, ref), memberId).id();
    }
    void state(long balance, long count) {
        tx(() -> {
            assertEquals(balance, em.find(Wallet.class, walletId).getBalance());
            assertEquals(count, em.createQuery(
                "select count(t) from WalletTransaction t where t.walletId=:id", Long.class)
                .setParameter("id", walletId).getSingleResult().longValue());
            return null;
        });
    }
    @Test void debitAndReturn() {
        UUID ref = UUID.randomUUID();
        call(WalletTransactionType.RESERVATION_DEBIT, 30, ref);
        state(70, 2);
        call(WalletTransactionType.RESERVATION_RETURN, 30, ref);
        state(100, 3);
    }
    @Test void insufficientBalanceLeavesNoHistory() {
        assertThrows(ConflictException.class, () -> call(WalletTransactionType.RESERVATION_DEBIT, 101, UUID.randomUUID()));
        state(100, 1);
    }
    @Test void returnWithoutDebitIsRejected() {
        assertThrows(ConflictException.class, () -> call(WalletTransactionType.RESERVATION_RETURN, 10, UUID.randomUUID()));
        state(100, 1);
    }
    @Test void excessiveReturnIsRejected() {
        UUID ref = UUID.randomUUID();
        call(WalletTransactionType.RESERVATION_DEBIT, 30, ref);
        assertThrows(ConflictException.class, () -> call(WalletTransactionType.RESERVATION_RETURN, 31, ref));
        state(70, 2);
    }
    @Test void duplicateChargeReturnsSameRecord() {
        UUID ref = UUID.randomUUID();
        UUID first = call(WalletTransactionType.PAYMENT_CHARGE, 20, ref);
        assertEquals(first, call(WalletTransactionType.PAYMENT_CHARGE, 20, ref));
        assertThrows(ConflictException.class, () -> call(WalletTransactionType.PAYMENT_CHARGE, 21, ref));
        state(120, 2);
    }
    @Test void duplicateDebitReturnsSameRecord() {
        UUID ref = UUID.randomUUID();
        UUID first = call(WalletTransactionType.RESERVATION_DEBIT, 30, ref);
        assertEquals(first, call(WalletTransactionType.RESERVATION_DEBIT, 30, ref));
        assertThrows(ConflictException.class, () -> call(WalletTransactionType.RESERVATION_DEBIT, 31, ref));
        state(70, 2);
    }
    @Test void partialReturnIsOnceOnly() {
        UUID ref = UUID.randomUUID();
        call(WalletTransactionType.RESERVATION_DEBIT, 30, ref);
        UUID first = call(WalletTransactionType.RESERVATION_RETURN, 20, ref);
        assertEquals(first, call(WalletTransactionType.RESERVATION_RETURN, 20, ref));
        assertThrows(ConflictException.class, () -> call(WalletTransactionType.RESERVATION_RETURN, 10, ref));
        state(90, 3);
    }
    @Test void outerFailureRollsBackBalanceAndHistory() {
        assertThrows(IllegalStateException.class, () -> tx(() -> {
            call(WalletTransactionType.RESERVATION_DEBIT, 30, UUID.randomUUID());
            em.flush();
            throw new IllegalStateException("forced outer transaction failure");
        }));
        state(100, 1);
    }
    @Test void historyInsertFailureRollsBackBalance() {
        tx(() -> {
            em.createNativeQuery("alter table wallet_transaction add constraint test_reject_13 check (amount <> 13)").executeUpdate();
            return null;
        });
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> call(WalletTransactionType.PAYMENT_CHARGE, 13, UUID.randomUUID()));
            state(100, 1);
        } finally {
            tx(() -> { em.createNativeQuery("alter table wallet_transaction drop constraint test_reject_13").executeUpdate(); return null; });
        }
    }
    List<Object> concurrent(java.util.function.IntFunction<UUID> operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i=0; i<2; i++) {
                final int index=i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException();
                    try { return operation.apply(index); }
                    catch (ConflictException e) { return e; }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> f : futures) results.add(f.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown(); pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
    @Test void simultaneousDifferentDebitsCannotOverspend() throws Exception {
        List<Object> results = concurrent(i -> call(WalletTransactionType.RESERVATION_DEBIT, 80, UUID.randomUUID()));
        assertEquals(1, results.stream().filter(UUID.class::isInstance).count());
        assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count());
        state(20, 2);
    }
    @Test void simultaneousDuplicateDebitIsAppliedOnce() throws Exception {
        UUID ref = UUID.randomUUID();
        List<Object> results = concurrent(i -> call(WalletTransactionType.RESERVATION_DEBIT, 30, ref));
        assertInstanceOf(UUID.class, results.get(0));
        assertEquals(results.get(0), results.get(1));
        state(70, 2);
    }
    @Test void simultaneousDuplicateReturnIsAppliedOnce() throws Exception {
        UUID ref = UUID.randomUUID();
        call(WalletTransactionType.RESERVATION_DEBIT, 30, ref);
        List<Object> results = concurrent(i -> call(WalletTransactionType.RESERVATION_RETURN, 30, ref));
        assertInstanceOf(UUID.class, results.get(0));
        assertEquals(results.get(0), results.get(1));
        state(100, 3);
    }
}
