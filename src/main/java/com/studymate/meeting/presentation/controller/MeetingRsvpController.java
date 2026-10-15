package com.studymate.meeting.presentation.controller;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.meeting.application.dto.request.MeetingRsvpCreateRequest;
import com.studymate.meeting.application.dto.request.MeetingRsvpUpdateRequest;
import com.studymate.meeting.application.dto.response.MeetingRsvpResponse;
import com.studymate.meeting.application.service.MeetingRsvpService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies/{studyId}/meetings/{meetingId}/rsvp")
public class MeetingRsvpController {
    private final MeetingRsvpService meetingRsvpService;

    @PostMapping("/me")
    public ResponseEntity<MeetingRsvpResponse> create(
            @PathVariable UUID studyId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody MeetingRsvpCreateRequest request
    ) {
        MeetingRsvpResponse response = meetingRsvpService.create(
                studyId,
                meetingId,
                principal.getMemberId(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<MeetingRsvpResponse> update(
            @PathVariable UUID studyId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody MeetingRsvpUpdateRequest request
    ) {
        MeetingRsvpResponse response = meetingRsvpService.update(
                studyId,
                meetingId,
                principal.getMemberId(),
                request
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MeetingRsvpResponse> getMyRsvp(
            @PathVariable UUID studyId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        MeetingRsvpResponse response = meetingRsvpService.getMyRsvp(
                studyId,
                meetingId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }
}
