package com.studymate.study.application.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record StudyMemberPageResponse(
        List<StudyMemberResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static StudyMemberPageResponse from(Page<StudyMemberResponse> result) {
        return new StudyMemberPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
