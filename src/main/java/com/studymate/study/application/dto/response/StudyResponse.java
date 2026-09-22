package com.studymate.study.application.dto.response;

import com.studymate.global.domain.ActivityRegion;
import com.studymate.study.domain.Study;
import com.studymate.study.domain.StudyStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record StudyResponse(
        UUID id,
        String title,
        String description,
        UUID leaderMemberId,
        int maxMembers,
        ActivityRegion activityRegion,
        StudyStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudyResponse from(Study study) {
        return new StudyResponse(
                study.getId(),
                study.getTitle(),
                study.getDescription(),
                study.getLeaderMemberId(),
                study.getMaxMembers(),
                study.getActivityRegion(),
                study.getStatus(),
                study.getCreatedAt(),
                study.getUpdatedAt()
        );
    }
}
