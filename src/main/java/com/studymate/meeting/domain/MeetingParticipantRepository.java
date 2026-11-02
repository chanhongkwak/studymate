package com.studymate.meeting.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingParticipantRepository {
    MeetingParticipant save(MeetingParticipant meetingParticipant);

    Optional<MeetingParticipant> findByMeetingIdAndMemberId(
            UUID meetingId,
            UUID memberId
    );

    List<MeetingParticipant> findAllByMeetingId(UUID meetingId);
}
