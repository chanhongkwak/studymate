package com.studymate.study.application.dto.request;

import java.util.UUID;

public record StudyLeadershipTransferRequest(
        UUID newLeaderMemberId
) {
}
