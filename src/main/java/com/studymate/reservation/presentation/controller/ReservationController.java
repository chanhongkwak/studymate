package com.studymate.reservation.presentation.controller;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.reservation.application.dto.request.ReservationAttendanceRequest;
import com.studymate.reservation.application.dto.request.ReservationCreateRequest;
import com.studymate.reservation.application.dto.request.ReservationQrDelegateRequest;
import com.studymate.reservation.application.dto.request.ReservationQrHandoverRequest;
import com.studymate.reservation.application.dto.request.ReservationQrScanRequest;
import com.studymate.reservation.application.dto.response.ReservationAttendanceResponse;
import com.studymate.reservation.application.dto.response.ReservationPageResponse;
import com.studymate.reservation.application.dto.response.ReservationQrCodeResponse;
import com.studymate.reservation.application.dto.response.ReservationQrScanResponse;
import com.studymate.reservation.application.dto.response.ReservationResponse;
import com.studymate.reservation.application.service.ReservationAttendanceService;
import com.studymate.reservation.application.service.ReservationQrCodeService;
import com.studymate.reservation.application.service.ReservationQrManagementService;
import com.studymate.reservation.application.service.ReservationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationAttendanceService reservationAttendanceService;
    private final ReservationQrCodeService reservationQrCodeService;
    private final ReservationQrManagementService reservationQrManagementService;

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> create(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody ReservationCreateRequest request
    ) {
        ReservationResponse response = reservationService.create(
                principal.getMemberId(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/studies/{studyId}/reservations/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        ReservationResponse response = reservationService.getReservation(
                studyId,
                reservationId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/studies/{studyId}/reservations/{reservationId}/attendance/me")
    public ResponseEntity<ReservationAttendanceResponse> getMyAttendance(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        ReservationAttendanceResponse response = reservationAttendanceService.getMyAttendance(
                studyId,
                reservationId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/studies/{studyId}/reservations/{reservationId}/attendance/check-in")
    public ResponseEntity<Void> checkIn(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody ReservationAttendanceRequest request
    ) {
        reservationQrCodeService.checkInWithGrant(
                studyId,
                reservationId,
                principal.getMemberId(),
                request.grantToken()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/studies/{studyId}/reservations/{reservationId}/attendance/check-out")
    public ResponseEntity<Void> checkOut(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody ReservationAttendanceRequest request
    ) {
        reservationQrCodeService.checkOutWithGrant(
                studyId,
                reservationId,
                principal.getMemberId(),
                request.grantToken()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/studies/{studyId}/reservations")
    public ResponseEntity<ReservationPageResponse> getReservations(
            @PathVariable UUID studyId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ReservationPageResponse response =
                reservationService.getReservations(
                        studyId,
                        principal.getMemberId(),
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/studies/{studyId}/reservations/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        ReservationResponse response = reservationService.cancel(
                studyId,
                reservationId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/studies/{studyId}/reservations/{reservationId}/attendance/qr/delegate")
    public ResponseEntity<Void> changeQrDelegate(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody ReservationQrDelegateRequest request
    ) {
        reservationQrManagementService.changeDelegate(
                studyId,
                reservationId,
                principal.getMemberId(),
                request.delegateParticipantId()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/studies/{studyId}/reservations/{reservationId}/attendance/qr/open")
    public ResponseEntity<Void> openQr(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        reservationQrManagementService.open(
                studyId,
                reservationId,
                principal.getMemberId()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/studies/{studyId}/reservations/{reservationId}/attendance/qr/handover")
    public ResponseEntity<Void> handoverQr(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody ReservationQrHandoverRequest request
    ) {
        reservationQrManagementService.handover(
                studyId,
                reservationId,
                principal.getMemberId(),
                request.nextParticipantId()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/studies/{studyId}/reservations/{reservationId}/attendance/qr")
    public ResponseEntity<ReservationQrCodeResponse> issueQr(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        ReservationQrCodeResponse response = reservationQrCodeService.issue(
                studyId,
                reservationId,
                principal.getMemberId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/studies/{studyId}/reservations/{reservationId}/attendance/qr/scan")
    public ResponseEntity<ReservationQrScanResponse> scanQr(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody ReservationQrScanRequest request
    ) {
        ReservationQrScanResponse response = reservationQrCodeService.scan(
                studyId,
                reservationId,
                principal.getMemberId(),
                request.token()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/studies/{studyId}/reservations/{reservationId}/attendance/operator/check-in")
    public ResponseEntity<Void> checkInAsOperator(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        reservationQrCodeService.checkInAsOperator(
                studyId,
                reservationId,
                principal.getMemberId()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/studies/{studyId}/reservations/{reservationId}/attendance/operator/check-out")
    public ResponseEntity<Void> checkOutAsOperator(
            @PathVariable UUID studyId,
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        reservationQrCodeService.checkOutAsOperator(
                studyId,
                reservationId,
                principal.getMemberId()
        );

        return ResponseEntity.noContent().build();
    }
}
