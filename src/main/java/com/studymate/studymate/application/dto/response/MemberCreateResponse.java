package com.studymate.studymate.application.dto.response;

import com.studymate.studymate.domain.ActivityRegion;
import com.studymate.studymate.domain.Member;
import com.studymate.studymate.domain.MemberRole;
import com.studymate.studymate.domain.MemberStatus;
import java.util.UUID;

public record MemberCreateResponse(
        UUID id,
        String email,
        String nickname,
        MemberRole role,
        MemberStatus status,
        ActivityRegion activityRegion,
        boolean activityRegionVerified
) {
    public static MemberCreateResponse from(Member member) {
        return new MemberCreateResponse(member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getStatus(),
                member.getActivityRegion(),
                member.isActivityRegionVerified());
    }
}
