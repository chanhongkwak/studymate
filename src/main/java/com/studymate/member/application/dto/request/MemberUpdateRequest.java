package com.studymate.member.application.dto.request;

import com.studymate.global.domain.ActivityRegion;

public record MemberUpdateRequest(
        String email,
        String password,
        String nickname,
        ActivityRegion activityRegion
) {
}
