package com.studymate.meeting.domain;

import com.studymate.global.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "meeting")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID studyId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = true, length = 5000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false, length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeetingStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;


    public Meeting(UUID studyId, String title, String description, LocalDateTime startAt, LocalDateTime endAt,
                   String location, UUID createdBy) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("제목은 최대 100자까지 가능합니다.");
        }
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("설명은 최대 5000자까지 가능합니다.");
        }
        if (startAt == null) {
            throw new IllegalArgumentException("시작시간은 필수입니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!startAt.isAfter(now)) {
            throw new IllegalArgumentException("시작 시간은 현재 시간보다 지나야 가능합니다.");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("종료시간은 필수입니다.");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("종료 시각은 시작 시각보다 이후여야 합니다.");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("지역은 필수입니다.");
        }
        if (location.length() > 255) {
            throw new IllegalArgumentException("지역은 255자까지 가능합니다.");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("생성자 ID는 필수입니다.");
        }

        this.studyId = studyId;
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;

        this.status = MeetingStatus.SCHEDULED;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public void update(
            String title, String description, LocalDateTime startAt, LocalDateTime endAt,
            String location, UUID updatedBy
    ) {

        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("설명은 최대 5000자까지 가능합니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!this.startAt.isAfter(now)) {
            throw new ConflictException("이미 시작한 모임은 수정할 수 없습니다.");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }
        if (this.status == MeetingStatus.CANCELED) {
            throw new ConflictException("취소된 모임은 수정할 수 없습니다.");
        }

        String nextTitle = title == null ? this.title : title;
        String nextDescription = description == null ? this.description : description;
        String nextLocation = location == null ? this.location : location;
        LocalDateTime nextStartAt = startAt == null ? this.startAt : startAt;
        LocalDateTime nextEndAt = endAt == null ? this.endAt : endAt;

        if (nextTitle.length() > 100) {
            throw new IllegalArgumentException("제목은 최대 100자까지 가능합니다.");
        }
        if (nextTitle.isBlank()) {
            throw new IllegalArgumentException("제목은 공백이 허용되지 않습니다.");
        }
        if (nextLocation.isBlank()) {
            throw new IllegalArgumentException("장소는 필수입니다.");
        }
        if (nextLocation.length() > 255) {
            throw new IllegalArgumentException("장소는 최대 255자까지 가능합니다.");
        }
        if (!nextStartAt.isAfter(now)) {
            throw new IllegalArgumentException("시작 시각은 현재 시각보다 이후여야 합니다.");
        }
        if (!nextEndAt.isAfter(nextStartAt)) {
            throw new IllegalArgumentException("종료 시각은 시작 시각보다 이후여야 합니다.");
        }

        this.title = nextTitle;
        this.description = nextDescription;
        this.location = nextLocation;
        this.startAt = nextStartAt;
        this.endAt = nextEndAt;
        this.updatedBy = updatedBy;

        this.updatedAt = now;
    }

    public void cancel(UUID canceledBy) {
        if (canceledBy == null) {
            throw new IllegalArgumentException("취소자ID는 필수입니다.");
        }
        if (this.status == MeetingStatus.CANCELED) {
            throw new ConflictException("이미 취소된 일정입니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!this.startAt.isAfter(now)) {
            throw new ConflictException("이미 시작된 일정입니다.");
        }

        this.status = MeetingStatus.CANCELED;
        this.updatedAt = now;
        this.updatedBy = canceledBy;
    }
}
