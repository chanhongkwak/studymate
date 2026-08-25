package com.studymate.studymate.application.dto.request;

import com.studymate.studymate.domain.ActivityRegion;

public record MemberCreateRequest(
        String email,
        String password,
        String nickname,
        ActivityRegion activityRegion
) {
}
