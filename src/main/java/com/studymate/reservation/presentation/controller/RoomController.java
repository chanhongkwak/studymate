package com.studymate.reservation.presentation.controller;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.reservation.application.dto.request.RoomCreateRequest;
import com.studymate.reservation.application.dto.request.RoomStatusUpdateRequest;
import com.studymate.reservation.application.dto.request.RoomUpdateRequest;
import com.studymate.reservation.application.dto.response.RoomPageResponse;
import com.studymate.reservation.application.dto.response.RoomResponse;
import com.studymate.reservation.application.service.RoomService;
import com.studymate.reservation.domain.room.RoomStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> create(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody RoomCreateRequest request
    ) {
        RoomResponse response = roomService.create(request, principal.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{roomId}")
    public ResponseEntity<RoomResponse> update(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody RoomUpdateRequest request
    ) {
        RoomResponse response = roomService.update(
                request,
                roomId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{roomId}/status")
    public ResponseEntity<RoomResponse> changeStatus(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody RoomStatusUpdateRequest status
    ) {
        RoomResponse response = roomService.changeStatus(
                roomId,
                status.status(),
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(
            @PathVariable UUID roomId
    ) {
        RoomResponse response = roomService.getRoom(roomId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<RoomPageResponse> getRooms(
            @RequestParam UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<RoomResponse> result = roomService.getRooms(
                branchId, page, size);

        return ResponseEntity.ok(RoomPageResponse.from(result));
    }

    @GetMapping("/admin")
    public ResponseEntity<RoomPageResponse> getRoomsForAdmin(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam UUID branchId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<RoomResponse> result = roomService.getRoomsForAdmin(
                principal.getMemberId(), branchId, status, page, size);

        return ResponseEntity.ok(RoomPageResponse.from(result));
    }
}
