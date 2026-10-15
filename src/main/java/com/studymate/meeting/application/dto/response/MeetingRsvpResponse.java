package com.studymate.meeting.application.dto.response;

import com.studymate.meeting.domain.MeetingRsvp;
import com.studymate.meeting.domain.MeetingRsvpStatus;
import java.util.UUID;

public record MeetingRsvpResponse(
        UUID meetingId,
        UUID memberId,
        boolean responded,
        MeetingRsvpStatus status,
        String reason
) {
    public static MeetingRsvpResponse from(MeetingRsvp meetingRsvp) {
        return new MeetingRsvpResponse(
                meetingRsvp.getMeetingId(),
                meetingRsvp.getMemberId(),
                true,
                meetingRsvp.getStatus(),
                meetingRsvp.getReason()
        );
    }

    public static MeetingRsvpResponse unanswered(UUID meetingId, UUID memberId) {
        return new MeetingRsvpResponse(meetingId, memberId, false, null, null);
    }
}
