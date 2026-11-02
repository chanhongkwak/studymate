package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.MeetingParticipant;
import com.studymate.meeting.domain.MeetingParticipantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MeetingParticipantRepositoryImpl implements MeetingParticipantRepository {
    private final JpaMeetingParticipantRepository jpaMeetingParticipantRepository;

    @Override
    public MeetingParticipant save(MeetingParticipant meetingParticipant) {
        return jpaMeetingParticipantRepository.save(meetingParticipant);
    }

    @Override
    public Optional<MeetingParticipant> findByMeetingIdAndMemberId(UUID meetingId, UUID memberId) {
        return jpaMeetingParticipantRepository.findByMeetingIdAndMemberId(meetingId, memberId);
    }

    @Override
    public List<MeetingParticipant> findAllByMeetingId(UUID meetingId) {
        return jpaMeetingParticipantRepository.findAllByMeetingId(meetingId);
    }
}
