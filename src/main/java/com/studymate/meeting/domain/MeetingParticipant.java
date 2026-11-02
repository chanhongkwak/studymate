package com.studymate.meeting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "meeting_participant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meeting_participant_meeting_member",
                columnNames = {"meeting_id", "member_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MeetingParticipant {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "meeting_id", nullable = false, updatable = false)
    private UUID meetingId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(name = "study_member_id", nullable = false, updatable = false)
    private UUID studyMemberId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    public MeetingParticipant(
            UUID meetingId,
            UUID memberId,
            UUID studyMemberId,
            UUID createdBy
    ) {
        if (meetingId == null) {
            throw new IllegalArgumentException("모임ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }
        if (studyMemberId == null) {
            throw new IllegalArgumentException("스터디회원ID는 필수입니다.");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("생성자ID는 필수입니다,");
        }

        this.meetingId = meetingId;
        this.memberId = memberId;
        this.studyMemberId = studyMemberId;

        this.createdAt = LocalDateTime.now(KOREA_ZONE);
        this.createdBy = createdBy;
    }


    // 응답 종료 때 이미 자격을 잃은 사람을 이후 현황의 분모에서 구분한다.
    private boolean excludedAtClosure;

    public void excludeAtClosure() {
        this.excludedAtClosure = true;
    }
}
