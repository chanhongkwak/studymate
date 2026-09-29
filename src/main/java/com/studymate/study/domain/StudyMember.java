package com.studymate.study.domain;

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
@Table(name = "study_members")
public class StudyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID studyId;

    @Column(nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StudyMemberStatus status;

    @Column(length = 255)
    private String applicationMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;

    public StudyMember(
            UUID studyId, UUID memberId, String applicationMessage
    ) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디 ID는 필수입니다.");
        }

        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }
        if (applicationMessage != null && applicationMessage.length() > 255) {
            throw new IllegalArgumentException("신청 메세지는 255자를 초과할 수 없습니다.");
        }

        this.studyId = studyId;
        this.memberId = memberId;
        this.applicationMessage = applicationMessage;

        this.status = StudyMemberStatus.PENDING_APPROVAL;

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.createdBy = memberId;
        this.updatedAt = now;
        this.updatedBy = memberId;
    }

    public void approve(UUID approvedBy) {
        if (approvedBy == null) {
            throw new IllegalArgumentException("승인자 ID는 필수입니다.");
        }

        if (this.status != StudyMemberStatus.PENDING_APPROVAL) {
            throw new ConflictException("승인 대기 상태에서만 승인할 수 있습니다.");
        }

        this.status = StudyMemberStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = approvedBy;
    }

    public void reject(UUID rejectedBy) {
        if (rejectedBy == null) {
            throw new IllegalArgumentException("처리자 ID는 필수입니다.");
        }

        if (this.status != StudyMemberStatus.PENDING_APPROVAL) {
            throw new ConflictException("승인 대기 상태에서만 거절할 수 있습니다.");
        }

        this.status = StudyMemberStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = rejectedBy;
    }


    public void leave(UUID leftBy) {
        if (leftBy == null) {
            throw new IllegalArgumentException("탈퇴자 ID는 필수입니다.");
        }

        if (this.status != StudyMemberStatus.ACTIVE) {
            throw new ConflictException("활동 상태에서만 탈퇴할 수 있습니다.");
        }

        this.status = StudyMemberStatus.LEFT;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = leftBy;

    }
}
