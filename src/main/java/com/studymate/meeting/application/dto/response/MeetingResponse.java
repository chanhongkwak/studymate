package com.studymate.meeting.application.dto.response;

import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record MeetingResponse(
        UUID id,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        UUID branchId,
        long maxCostPerPerson,
        MeetingStatus status
) {
    public static MeetingResponse from(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getStartAt(),
                meeting.getEndAt(),
                meeting.getBranchId(),
                meeting.getMaxCostPerPerson(),
                meeting.getStatus()
        );
    }
}
