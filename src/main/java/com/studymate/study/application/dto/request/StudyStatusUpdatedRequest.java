package com.studymate.study.application.dto.request;

import com.studymate.study.domain.StudyStatus;

public record StudyStatusUpdatedRequest(
        StudyStatus status
) {

}
