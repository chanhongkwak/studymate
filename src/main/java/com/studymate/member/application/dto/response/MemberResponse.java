package com.studymate.member.application.dto.response;

import com.studymate.member.domain.ActivityRegion;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRole;
import com.studymate.member.domain.MemberStatus;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        String email,
        String nickname,
        MemberRole role,
        MemberStatus status,
        ActivityRegion activityRegion,
        boolean activityRegionVerified
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole(),
                member.getStatus(),
                member.getActivityRegion(),
                member.isActivityRegionVerified());
    }
}
