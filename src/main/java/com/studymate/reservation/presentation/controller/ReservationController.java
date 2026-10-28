package com.studymate.reservation.presentation.controller;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.reservation.application.dto.request.ReservationCreateRequest;
import com.studymate.reservation.application.dto.response.ReservationPageResponse;
import com.studymate.reservation.application.dto.response.ReservationResponse;
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
}
