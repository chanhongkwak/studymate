package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.MeetingRsvp;
import com.studymate.meeting.domain.MeetingRsvpRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MeetingRsvpRepositoryImpl implements MeetingRsvpRepository {
    private final JpaMeetingRsvpRepository jpaMeetingRsvpRepository;

    @Override
    public MeetingRsvp save(MeetingRsvp meetingRsvp) {
        return jpaMeetingRsvpRepository.save(meetingRsvp);
    }

    @Override
    public Optional<MeetingRsvp> findByMeetingIdAndMemberId(UUID meetingId, UUID memberId) {
        return jpaMeetingRsvpRepository.findByMeetingIdAndMemberId(meetingId, memberId);
    }
}
