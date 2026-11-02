package com.studymate.meeting.application.service;

import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingRepository;
import com.studymate.study.domain.StudyRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingDeadlineWorker {
    private final StudyRepository studies;
    private final MeetingRepository meetings;
    private final MeetingResponseService responses;

    @Transactional
    public void process(UUID studyId) {
        if (studies.findByIdForUpdate(studyId).isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).truncatedTo(ChronoUnit.MICROS);
        for (Meeting meeting : meetings.findExpiredProposals(studyId, now.plusHours(24))) {
            responses.expireUnreserved(meeting, now);
        }
        for (Meeting meeting : meetings.findDueForResponseClosure(studyId, now)) {
            responses.closeIfDeadlineReachedBySystem(meeting);
        }
    }
}
