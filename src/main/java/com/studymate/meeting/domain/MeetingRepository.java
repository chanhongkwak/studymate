package com.studymate.meeting.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingRepository {
    Meeting save(Meeting meeting);

    boolean existOverlappingMeeting(
            UUID studyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    Optional<Meeting> findById(UUID meetingId);

    Page<Meeting> findAllByStudyId(
            UUID studyId,
            Pageable pageable
    );

    Page<Meeting> findAllByStudyIdAndEndAtLessThanEqual(
            UUID studyId,
            LocalDateTime endAt,
            Pageable pageable
    );

    boolean existsOverlappingMeetingExcludingId(
            UUID studyId,
            UUID meetingId,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    List<Meeting> findAllByStudyIdAndStatusAndStartAtAfter(
            UUID studyId,
            MeetingStatus status,
            LocalDateTime startAt
    );
}
