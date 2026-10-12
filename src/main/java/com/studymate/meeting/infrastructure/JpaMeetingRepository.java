package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMeetingRepository extends JpaRepository<Meeting, UUID> {
    @Query("""
        select (count(m) > 0)
        from Meeting m
        where m.studyId = :studyId
          and m.status <> :excludedStatus
          and m.startAt < :endAt
          and m.endAt > :startAt
        """)
    boolean existsOverlappingMeeting(
            @Param("studyId") UUID studyId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludedStatus") MeetingStatus excludedStatus
    );

   Optional <Meeting> findById(UUID meetingId);
}
