package com.studymate.wallet.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.wallet.application.dto.request.WalletTransactionRequest;
import com.studymate.wallet.application.dto.response.WalletTransactionResponse;
import com.studymate.wallet.domain.Wallet;
import com.studymate.wallet.domain.WalletRepository;
import com.studymate.wallet.domain.WalletTransaction;
import com.studymate.wallet.domain.WalletTransactionRepository;
import com.studymate.wallet.domain.WalletTransactionType;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletRepository walletRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public WalletTransactionResponse create(WalletTransactionRequest request, UUID updateBy) {
        if (request == null) {
            throw new IllegalArgumentException("거래 요청은 필수입니다.");
        }
        if (request.walletId() == null) {
            throw new IllegalArgumentException("지갑ID는 필수입니다.");
        }
        if (request.walletTransactionType() == null) {
            throw new IllegalArgumentException("거래 종류는 필수입니다.");
        }
        if (request.amount() == null || request.amount() < 1) {
            throw new IllegalArgumentException("거래 금액은 1MC 이상이어야 합니다.");
        }
        if (request.referenceId() == null) {
            throw new IllegalArgumentException("거래원인ID는 필수입니다.");
        }
        Wallet wallet = walletRepository.findByIdForUpdate(request.walletId())
                .orElseThrow(() -> new NotFoundException("해당 지갑을 찾을 수 없습니다."));

        String idempotencyKey =
                request.walletTransactionType().name()
                        + ":" + request.referenceId()
                        + ":" + wallet.getId();

        Optional<WalletTransaction> existingTransaction = walletTransactionRepository.findByIdempotencyKey(
                idempotencyKey);

        if (existingTransaction.isPresent()) {
            WalletTransaction existing = existingTransaction.get();

            if (existing.getAmount() != request.amount().longValue()) {
                throw new ConflictException(("이미 처리된 거래의 금액과 다릅니다."));
            }

            return WalletTransactionResponse.from(existing);
        }

        if (request.walletTransactionType()
                == WalletTransactionType.RESERVATION_DEBIT) {

            Member member = memberRepository.findByIdForUpdate(wallet.getMemberId())
                    .orElseThrow(() ->
                            new NotFoundException("회원을 찾을 수 없습니다."));

            if (member.getDeletedAt() != null) {
                throw new NotFoundException("회원을 찾을 수 없습니다.");
            }

            if (member.getStatus() != MemberStatus.ACTIVE) {
                throw new ForbiddenException("활성 회원만 예약 비용을 차감할 수 있습니다.");
            }
        }
        if (request.walletTransactionType()
                == WalletTransactionType.RESERVATION_RETURN) {
            String debitKey =
                    WalletTransactionType.RESERVATION_DEBIT.name()
                            + ":" + request.referenceId()
                            + ":" + wallet.getId();

            WalletTransaction originalDebit =
                    walletTransactionRepository.findByIdempotencyKey(debitKey)
                            .orElseThrow(() -> new ConflictException(("반환할 원래 차감 내역이 없습니다.")));

            if (request.amount() >
                    originalDebit.getAmount()) {
                throw new ConflictException("반환 금액은 원래 차감 금액을 초과할 수 없습니다.");
            }
        }

        switch (request.walletTransactionType()) {
            case PAYMENT_CHARGE, RESERVATION_RETURN -> wallet.credit(request.amount(), updateBy);

            case RESERVATION_DEBIT -> wallet.debit(request.amount(), updateBy);
        }

        WalletTransaction transaction = new WalletTransaction(
                wallet.getId(),
                request.walletTransactionType(),
                request.amount(),
                wallet.getBalance(),
                request.referenceId(),
                idempotencyKey,
                updateBy
        );

        WalletTransaction savedTransaction =
                walletTransactionRepository.save(transaction);

        return WalletTransactionResponse.from(savedTransaction);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getMyTransactions(
            UUID memberId,
            int page,
            int size
    ) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("페이지는 0이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1~100이어야 합니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        Wallet wallet = walletRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new NotFoundException("지갑을 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<WalletTransaction> transactions =
                walletTransactionRepository.findAllByWalletId(wallet.getId(), pageable);

        return transactions.map(WalletTransactionResponse::from);
    }
}
