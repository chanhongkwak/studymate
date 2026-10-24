package com.studymate.wallet.application.dto.request;

import java.util.UUID;

public record WalletCreateRequest(
        UUID memberId
) {
}
