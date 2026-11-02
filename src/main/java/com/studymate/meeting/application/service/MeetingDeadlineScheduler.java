package com.studymate.meeting.application.service;

import com.studymate.meeting.domain.MeetingRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "studymate.meeting.deadlines.enabled", matchIfMissing = true)
public class MeetingDeadlineScheduler {
    private final MeetingRepository meetings;
    private final MeetingDeadlineWorker worker;

    @Scheduled(fixedDelayString = "${studymate.meeting.deadlines.delay-ms:30000}")
    public void run() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).truncatedTo(ChronoUnit.MICROS);
        for (UUID studyId : meetings.findStudiesWithDueMeetings(now, now.plusHours(24))) {
            try {
                worker.process(studyId);
            } catch (RuntimeException failure) {
                log.error("모임 마감 처리 실패: studyId={}", studyId, failure);
            }
        }
    }
}
