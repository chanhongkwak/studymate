package com.studymate.meeting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "meeting_rsvp",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meeting_rsvp_meeting_member",
                columnNames = {"meeting_id", "member_id"}
        )
)
public class MeetingRsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MeetingRsvpStatus status;

    @Column(nullable = true, length = 255)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;

    public MeetingRsvp(
            UUID meetingId,
            UUID memberId,
            MeetingRsvpStatus status,
            String reason,
            UUID createdBy
    ) {
        if (meetingId == null) {
            throw new IllegalArgumentException("모임ID는 필수입니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원ID는 필수입니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("참석 응답 상태는 필수입니다.");
        }
        if (status == MeetingRsvpStatus.NOT_ATTENDING) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("불참 시 사유는 필수입니다.");
            }
            if (reason.length() > 255) {
                throw new IllegalArgumentException("사유는 255자를 초과하지 못합니다.");
            }
        }
        if(createdBy == null){
            throw new IllegalArgumentException("생성자ID는 필수입니다.");
        }

        this.meetingId = meetingId;
        this.memberId = memberId;
        this.status = status;
        this.reason = status == MeetingRsvpStatus.NOT_ATTENDING ? reason : null;

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public void update(
            MeetingRsvpStatus status,
            String reason,
            UUID updatedBy
    ) {
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자 ID는 필수입니다.");
        }

        MeetingRsvpStatus nextStatus =
                status == null ? this.status : status;

        String nextReason =
                reason == null ? this.reason : reason;

        if (nextStatus == MeetingRsvpStatus.NOT_ATTENDING) {
            if (nextReason == null || nextReason.isBlank()) {
                throw new IllegalArgumentException("불참 시 사유는 필수입니다.");
            }
            if (nextReason.length() > 255) {
                throw new IllegalArgumentException("사유는 255자를 초과하지 못합니다.");
            }
        } else {
            nextReason = null;
        }

        this.status = nextStatus;
        this.reason = nextReason;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }
}
