package com.studymate.study.application.dto.request;

import com.studymate.global.domain.ActivityRegion;
import com.studymate.study.domain.StudyStatus;
import java.util.UUID;

public record StudyUpdateRequest(
        String title,
        String description,
        Integer maxMembers,
        ActivityRegion activityRegion
) {
}
