package com.studymate.meeting.domain;

import java.util.Optional;
import java.util.UUID;

public interface MeetingRsvpRepository {
    MeetingRsvp save(MeetingRsvp meetingRsvp);

    Optional<MeetingRsvp> findByMeetingIdAndMemberId(UUID meetingId, UUID memberId);
}
