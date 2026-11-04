package com.studymate.reservation.application.dto.response;

import com.studymate.reservation.domain.reservation.AttendanceStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationAttendanceResponse(
        LocalDateTime checkedInAt,
        LocalDateTime checkedOutAt,
        List<Session> sessions
) {
    public record Session(
            LocalDateTime startAt,
            AttendanceStatus status
    ){}
}
