package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.MeetingRsvp;
import com.studymate.meeting.domain.MeetingRsvpRepository;
import java.util.List;
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

    @Override
    public void deleteByMeetingIdAndMemberIdNot(UUID meetingId, UUID memberId) {
        jpaMeetingRsvpRepository.deleteByMeetingIdAndMemberIdNot(meetingId, memberId);
    }

    @Override
    public List<MeetingRsvp> findAllByMeetingId(UUID meetingId) {
        return jpaMeetingRsvpRepository.findAllByMeetingId(meetingId);
    }

    @Override
    public void deleteByMeetingIdAndMemberId(UUID meetingId, UUID memberId) {
        jpaMeetingRsvpRepository.deleteByMeetingIdAndMemberId(meetingId, memberId);
    }
}
