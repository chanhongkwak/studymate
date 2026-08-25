package com.studymate.studymate.application.dto.request;

import com.studymate.studymate.domain.ActivityRegion;

public record MemberUpdateRequest(
        String email,
        String password,
        String nickname,
        ActivityRegion activityRegion
) {
}
