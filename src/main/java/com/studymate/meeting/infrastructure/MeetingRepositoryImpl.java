package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.meeting.domain.MeetingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<Meeting> findAllByStudyId(UUID studyId, Pageable pageable) {
        return jpaMeetingRepository.findAllByStudyId(studyId, pageable);
    }

    @Override
    public Page<Meeting> findAllByStudyIdAndEndAtLessThanEqual(UUID studyId, LocalDateTime endAt, Pageable pageable) {
        return jpaMeetingRepository.findAllByStudyIdAndEndAtLessThanEqual(
                studyId, endAt, pageable
        );
    }

    @Override
    public boolean existsOverlappingMeetingExcludingId(
            UUID studyId,
            UUID meetingId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return jpaMeetingRepository.existsOverlappingMeetingExcludingId(
                studyId,
                meetingId,
                startAt,
                endAt,
                MeetingStatus.CANCELED
        );
    }

    @Override
    public List<Meeting> findAllByStudyIdAndStatusAndStartAtAfter(UUID studyId, MeetingStatus status,
                                                                  LocalDateTime startAt) {
        return jpaMeetingRepository.findAllByStudyIdAndStatusAndStartAtAfter(
                studyId, status, startAt
        );
    }

    @Override
    public List<Meeting> findAllByStudyIdAndStatusInAndStartAtAfter(UUID studyId, List<MeetingStatus> statuses,
                                                                    LocalDateTime startAt) {
        return jpaMeetingRepository.findAllByStudyIdAndStatusInAndStartAtAfter(studyId, statuses, startAt);
    }
}
