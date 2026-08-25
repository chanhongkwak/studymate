package com.studymate.studymate.application.dto.response;

import com.studymate.studymate.domain.ActivityRegion;
import com.studymate.studymate.domain.Member;

public record MemberUpdateResponse(
        String email,
        String nickname,
        ActivityRegion activityRegion
) {
    public static MemberUpdateResponse from(Member member){
        return new MemberUpdateResponse(
                member.getEmail(),
                member.getNickname(),
                member.getActivityRegion()
        );
    }
}
