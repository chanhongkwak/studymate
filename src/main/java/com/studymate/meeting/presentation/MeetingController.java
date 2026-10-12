package com.studymate.meeting.presentation;

import com.studymate.auth.infrastructure.MemberPrincipal;
import com.studymate.meeting.application.MeetingService;
import com.studymate.meeting.application.dto.request.MeetingCreateRequest;
import com.studymate.meeting.application.dto.request.MeetingUpdateRequest;
import com.studymate.meeting.application.dto.response.MeetingPageResponse;
import com.studymate.meeting.application.dto.response.MeetingResponse;
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

    @GetMapping
    public ResponseEntity<MeetingPageResponse> getMeetings(
            @PathVariable UUID studyId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MeetingResponse> meetings = meetingService.getMeetings(
                studyId,
                principal.getMemberId(),
                page,
                size
        );

        MeetingPageResponse response = MeetingPageResponse.from(meetings);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{meetingId}")
    public ResponseEntity<MeetingResponse> updateMeeting(
            @PathVariable UUID studyId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody MeetingUpdateRequest request
    ) {
        MeetingResponse response = meetingService.update(
                studyId,
                meetingId,
                principal.getMemberId(),
                request
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{meetingId}/cancel")
    public ResponseEntity<MeetingResponse> cancelMeeting(
            @PathVariable UUID studyId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        MeetingResponse response = meetingService.cancel(
                studyId,
                meetingId,
                principal.getMemberId()
        );

        return ResponseEntity.ok(response);
    }

}
