package com.studymate.meeting.application.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record MeetingCreateRequest(
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        UUID branchId,
        Long maxCostPerPerson
) {
}
