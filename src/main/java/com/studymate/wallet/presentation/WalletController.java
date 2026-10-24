package com.studymate.wallet.presentation;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.wallet.application.dto.response.WalletResponse;
import com.studymate.wallet.application.dto.response.WalletTransactionPageResponse;
import com.studymate.wallet.application.dto.response.WalletTransactionResponse;
import com.studymate.wallet.application.service.WalletService;
import com.studymate.wallet.application.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    private final WalletTransactionService walletTransactionService;

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        WalletResponse response = walletService.getMyWallet(principal.getMemberId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<WalletTransactionPageResponse>
    getMyTransactions(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<WalletTransactionResponse> transactions =
                walletTransactionService.getMyTransactions(
                        principal.getMemberId(),
                        page,
                        size
                );

        return ResponseEntity.ok(
                WalletTransactionPageResponse.from(transactions)
        );
    }
}
