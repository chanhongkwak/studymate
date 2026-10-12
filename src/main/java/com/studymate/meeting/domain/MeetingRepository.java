package com.studymate.meeting.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository {
    Meeting save(Meeting meeting);

    boolean existOverlappingMeeting(
            UUID studyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    Optional<Meeting> findById(UUID meetingId);
}
