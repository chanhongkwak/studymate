package com.studymate.meeting.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRsvpRepository {
    MeetingRsvp save(MeetingRsvp meetingRsvp);

    Optional<MeetingRsvp> findByMeetingIdAndMemberId(UUID meetingId, UUID memberId);

    List<MeetingRsvp> findAllByMeetingId(UUID meetingId);

    void deleteByMeetingId(
            UUID meetingId
    );

    void deleteByMeetingIdAndMemberId(
            UUID meetingId,
            UUID memberId
    );

    void deleteByMeetingId(
            UUID meetingId
    );
}
