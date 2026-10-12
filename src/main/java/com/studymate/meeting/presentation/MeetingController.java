package com.studymate.meeting.presentation;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.meeting.application.MeetingService;
import com.studymate.meeting.application.dto.request.MeetingCreateRequest;
import com.studymate.meeting.application.dto.response.MeetingResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies/{studyId}/meetings")
public class MeetingController {
    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(
            @PathVariable UUID studyId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody MeetingCreateRequest request
    ) {
        MeetingResponse response = meetingService.create(
                studyId,
                principal.getMemberId(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingResponse> getMeeting(
            @PathVariable UUID studyId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        MeetingResponse response = meetingService.getMeeting(
                studyId,
                meetingId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }
}
