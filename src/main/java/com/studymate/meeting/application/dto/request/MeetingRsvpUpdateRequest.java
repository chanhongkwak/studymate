package com.studymate.meeting.application.dto.request;

import com.studymate.meeting.domain.MeetingRsvpStatus;

public record MeetingRsvpUpdateRequest(
        MeetingRsvpStatus status,
        String reason
) {
}
