package com.studymate.wallet.application.service;

import com.studymate.global.exception.ConflictException;
import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberStatus;
import com.studymate.wallet.application.dto.request.WalletCreateRequest;
import com.studymate.wallet.application.dto.response.WalletResponse;
import com.studymate.wallet.domain.Wallet;
import com.studymate.wallet.domain.WalletRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public WalletResponse create(WalletCreateRequest request, UUID createdBy) {
        if (request == null) {
            throw new IllegalArgumentException("지갑 생성 요청은 필수입니다.");
        }
        if (request.memberId() == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(request.memberId())
                .orElseThrow(() -> new NotFoundException("해당 회원이 존재하지 않습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원이 아닙니다.");
        }

        if (walletRepository.existsByMemberId(member.getId())) {
            throw new ConflictException("이미 지갑이 존재합니다.");
        }

        Wallet wallet = new Wallet(
                member.getId(), createdBy
        );

        walletRepository.save(wallet);

        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(UUID memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));

        Wallet wallet = walletRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new NotFoundException("해당 지갑을 찾을 수 없습니다."));

        return WalletResponse.from(wallet);
    }

}
