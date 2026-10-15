package com.studymate.meeting.application.dto.response;

import java.util.List;
import java.util.UUID;

public record MeetingRsvpSummaryResponse(
        UUID meetingId,
        int attendingCount,
        int notAttendingCount,
        int unansweredCount,
        List<MeetingRsvpResponse> members
) {
}
