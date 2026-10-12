package com.studymate.meeting.application.dto.request;

import java.time.LocalDateTime;

public record MeetingUpdateRequest(
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String location
) {
}
