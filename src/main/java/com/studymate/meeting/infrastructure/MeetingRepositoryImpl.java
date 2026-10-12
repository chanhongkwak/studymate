package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.meeting.domain.MeetingStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MeetingRepositoryImpl implements MeetingRepository {

    private final JpaMeetingRepository jpaMeetingRepository;

    @Override
    public Meeting save(Meeting meeting) {
        return jpaMeetingRepository.save(meeting);
    }

    @Override
    public boolean existOverlappingMeeting(UUID studyId, LocalDateTime startAt, LocalDateTime endAt) {
        return jpaMeetingRepository.existsOverlappingMeeting(studyId, startAt, endAt, MeetingStatus.CANCELED);
    }

    @Override
    public Optional<Meeting> findById(UUID meetingId) {
        return jpaMeetingRepository.findById(meetingId);
    }
}
