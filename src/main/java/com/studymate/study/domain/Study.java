package com.studymate.study.domain;

import com.studymate.global.domain.ActivityRegion;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "studies")
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false)
    private UUID leaderMemberId;

    @Column(nullable = false)
    private int maxMembers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActivityRegion activityRegion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StudyStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;

    private LocalDateTime deletedAt;

    private UUID deletedBy;

    public Study(
            String title,
            String description,
            UUID leaderMemberId,
            int maxMembers,
            ActivityRegion activityRegion
    ) {
        validate(title, description, maxMembers, activityRegion);

        if (leaderMemberId == null) {
            throw new IllegalArgumentException(("스터디장 ID는 필수입니다."));
        }

        LocalDateTime now = LocalDateTime.now();

        this.title = title;
        this.description = description;
        this.leaderMemberId = leaderMemberId;
        this.maxMembers = maxMembers;
        this.activityRegion = activityRegion;

        this.status = StudyStatus.RECRUITING;

        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = leaderMemberId;
        this.updatedBy = leaderMemberId;
    }

    public void update(
            String title,
            String description,
            Integer maxMembers,
            ActivityRegion activityRegion,
            UUID updatedBy
    ) {
        if (this.deletedAt != null) {
            throw new ConflictException("삭제된 스터디는 수정할 수 없습니다.");
        }

        if (this.status == StudyStatus.ENDED) {
            throw new ConflictException("종료된 스터디는 수정할 수 없습니다.");
        }

        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자 ID는 필수입니다.");
        }

        String nextTitle = title == null ? this.title : title;
        String nextDescription = description == null ? this.description : description;
        int nextMaxMembers = maxMembers == null ? this.maxMembers : maxMembers;
        ActivityRegion nextActivityRegion = activityRegion == null ? this.activityRegion : activityRegion;

        validate(
                nextTitle,
                nextDescription,
                nextMaxMembers,
                nextActivityRegion
        );

        this.title = nextTitle;
        this.description = nextDescription;
        this.maxMembers = nextMaxMembers;
        this.activityRegion = nextActivityRegion;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    public void changeStatus(StudyStatus nextStatus, UUID updatedBy) {
        if (this.deletedAt != null) {
            throw new ConflictException("삭제된 스터디는 상태를 변경할 수 없습니다.");
        }

        if (nextStatus == null) {
            throw new IllegalArgumentException("변경할 상태는 필수입니다.");
        }

        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자 ID는 필수입니다.");
        }

        if (this.status == nextStatus) {
            return;
        }

        if (this.status == StudyStatus.ENDED) {
            throw new ConflictException("종료된 스터디는 상태를 변경할 수 없습니다.");
        }

        this.status = nextStatus;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    public void delete(UUID deletedBy) {
        if (this.deletedAt != null) {
            throw new ConflictException("이미 삭제된 스터디입니다.");
        }

        if (deletedBy == null) {
            throw new IllegalArgumentException("삭제자 ID는 필수입니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        this.deletedAt = now;
        this.deletedBy = deletedBy;
        this.updatedAt = now;
        this.updatedBy = deletedBy;
    }

    private void validate(
            String title,
            String description,
            int maxMembers,
            ActivityRegion activityRegion
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("스터디 이름은 필수입니다.");
        }

        if (title.length() > 100) {
            throw new IllegalArgumentException(
                    "스터디 이름은 100자를 초과할 수 없습니다."
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("스터디 소개는 필수입니다.");
        }

        if (description.length() > 5000) {
            throw new IllegalArgumentException(
                    "스터디 소개는 5000자를 초과할 수 없습니다."
            );
        }

        if (maxMembers < 2) {
            throw new IllegalArgumentException(
                    "스터디 정원은 스터디장을 포함해 최소 2명입니다."
            );
        }

        if (activityRegion == null) {
            throw new IllegalArgumentException("활동 지역은 필수입니다.");
        }
    }
}

