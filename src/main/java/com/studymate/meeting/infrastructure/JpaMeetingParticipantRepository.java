package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.MeetingParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {
    Optional<MeetingParticipant> findByMeetingIdAndMemberId(
            UUID meetingId,
            UUID memberId
    );

    List<MeetingParticipant> findAllByMeetingId(UUID meetingId);
}
