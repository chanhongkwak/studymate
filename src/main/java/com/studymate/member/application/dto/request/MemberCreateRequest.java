package com.studymate.member.application.dto.request;

import com.studymate.member.domain.ActivityRegion;

public record MemberCreateRequest(
        String email,
        String password,
        String nickname,
        ActivityRegion activityRegion
) {
}
