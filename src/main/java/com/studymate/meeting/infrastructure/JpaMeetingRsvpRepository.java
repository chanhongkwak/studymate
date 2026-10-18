package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.MeetingRsvp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeetingRsvpRepository extends JpaRepository<MeetingRsvp, UUID> {
    Optional<MeetingRsvp> findByMeetingIdAndMemberId(UUID meetingId, UUID memberId);

    List<MeetingRsvp> findAllByMeetingId(UUID meetingId);

    void deleteByMeetingId(
            UUID meetingId
    );

    void deleteByMeetingIdAndMemberId(
            UUID meetingId,
            UUID memberId
    );
}
