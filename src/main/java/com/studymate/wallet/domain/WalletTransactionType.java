package com.studymate.wallet.domain;

public enum WalletTransactionType {
    PAYMENT_CHARGE,       // 결제에 따른 MC 충전
    RESERVATION_DEBIT,     // 예약 비용 차감
    RESERVATION_RETURN     // 예약 취소에 따른 MC 반환
}
