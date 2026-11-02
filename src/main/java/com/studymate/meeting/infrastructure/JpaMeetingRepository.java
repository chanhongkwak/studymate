package com.studymate.meeting.infrastructure;

import com.studymate.meeting.domain.Meeting;
import com.studymate.meeting.domain.MeetingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Optional<Meeting> findById(UUID meetingId);

    Page<Meeting> findAllByStudyId(UUID studyId, Pageable pageable);

    Page<Meeting> findAllByStudyIdAndEndAtLessThanEqual(UUID studyId, LocalDateTime endAt, Pageable pageable);

    @Query("""
            select (count(m) > 0)
            from Meeting m
            where m.studyId = :studyId
              and m.id <> :meetingId
              and m.status <> :excludedStatus
              and m.startAt < :endAt
              and m.endAt > :startAt
            """)
    boolean existsOverlappingMeetingExcludingId(
            @Param("studyId") UUID studyId,
            @Param("meetingId") UUID meetingId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludedStatus") MeetingStatus excludedStatus
    );

    List<Meeting> findAllByStudyIdAndStatusAndStartAtAfter(
            UUID studyId,
            MeetingStatus status,
            LocalDateTime startAt
    );

    List<Meeting> findAllByStudyIdAndStatusInAndStartAtAfter(
            UUID studyId,
            List<MeetingStatus> statuses,
            LocalDateTime startAt
    );

    @Query("select m.studyId from Meeting m where m.id = :meetingId")
    Optional<UUID> findStudyIdById(@Param("meetingId") UUID meetingId);

    @Query("""
        select m
        from Meeting m
        where m.studyId = :studyId
          and m.status = :status
          and m.responseClosedAt is null
          and m.responseDeadlineAt <= :cutoff
        """)
    List<Meeting> findDueForResponseClosure(
            @Param("studyId") UUID studyId,
            @Param("status") MeetingStatus status,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Query("""
        select m
        from Meeting m
        where m.studyId = :studyId
          and m.status = :status
          and m.responseClosedAt is null
          and m.responseDeadlineAt > :cutoff
          and exists (
              select p.id
              from MeetingParticipant p
              where p.meetingId = m.id
                and p.studyMemberId = :studyMemberId
          )
        """)
    List<Meeting> findOpenForParticipantDeparture(
            @Param("studyId") UUID studyId,
            @Param("studyMemberId") UUID studyMemberId,
            @Param("status") MeetingStatus status,
            @Param("cutoff") LocalDateTime cutoff
    );
    @Query("""
        select distinct m.studyId from Meeting m, Study s
        where s.id = m.studyId and s.deletedAt is null and m.status = :status
          and ((m.responseClosedAt is null and m.responseDeadlineAt <= :now)
               or m.startAt < :latestStart)
        """)
    List<UUID> findStudiesWithDueMeetings(MeetingStatus status, LocalDateTime now, LocalDateTime latestStart);

    List<Meeting> findAllByStudyIdAndStatusAndStartAtBefore(UUID studyId, MeetingStatus status,
                                                            LocalDateTime latestStart);
}
