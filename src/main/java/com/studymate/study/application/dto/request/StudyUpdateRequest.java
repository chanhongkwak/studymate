package com.studymate.study.application.dto.request;

import com.studymate.global.domain.ActivityRegion;

public record StudyUpdateRequest(
        String title,
        String description,
        Integer maxMembers,
        ActivityRegion activityRegion
) {
}
