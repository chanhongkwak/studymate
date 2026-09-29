package com.studymate.study.application.dto.response;

import com.studymate.study.domain.StudyMember;
import com.studymate.study.domain.StudyMemberStatus;
import java.util.UUID;

public record StudyMemberResponse(
        UUID id,
        UUID studyId,
        UUID memberId,
        StudyMemberStatus status,
        String applicationMessage
) {
    public static StudyMemberResponse from(StudyMember studyMember) {
        return new StudyMemberResponse(
                studyMember.getId(),
                studyMember.getStudyId(),
                studyMember.getMemberId(),
                studyMember.getStatus(),
                studyMember.getApplicationMessage()
        );
    }
}
