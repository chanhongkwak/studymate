package com.studymate.study.application.dto.request;

import com.studymate.global.domain.ActivityRegion;

public record StudyCreateRequest(
        String title,
        String description,
        Integer maxMembers,
        ActivityRegion activityRegion
) {
}
