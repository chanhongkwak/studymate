package com.studymate.meeting.application.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record MeetingPageResponse(
        List<MeetingResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static MeetingPageResponse from(
            Page<MeetingResponse> meetings
    ){
        return new MeetingPageResponse(
                meetings.getContent(),
                meetings.getNumber(),
                meetings.getSize(),
                meetings.getTotalElements(),
                meetings.getTotalPages()
        );
    }
}
