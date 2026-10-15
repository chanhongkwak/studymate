package com.studymate.meeting.application.dto.request;

import com.studymate.meeting.domain.MeetingRsvpStatus;

public record MeetingRsvpCreateRequest(
        MeetingRsvpStatus status,
        String reason
) {
}
