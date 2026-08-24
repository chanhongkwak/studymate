package com.studymate.studymate.domain;

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
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActivityRegion activityRegion;

    @Column(nullable = false)
    private boolean activityRegionVerified;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private UUID updatedBy;

    private LocalDateTime deletedAt;

    private UUID deletedBy;

    public Member(String email, String encodedPassword, String nickname, ActivityRegion activityRegion) {
        LocalDateTime now = LocalDateTime.now();

        validate(email, encodedPassword, nickname, activityRegion);

        this.email = email;
        this.password = encodedPassword;
        this.nickname = nickname;
        this.role = MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
        this.activityRegion = activityRegion;
        this.activityRegionVerified = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String email, String encodedPassword, String nickname, ActivityRegion activityRegion) {
        String nextEmail = email == null ? this.email : email;
        String nextEncodedPassword = encodedPassword == null ? this.password : encodedPassword;
        String nextNickname = nickname == null ? this.nickname : nickname;
        ActivityRegion nextActivityRegion = activityRegion == null ? this.activityRegion : activityRegion;

        validate(nextEmail, nextEncodedPassword, nextNickname, nextActivityRegion);
        boolean regionChanged = activityRegion != null && activityRegion != this.activityRegion;

        this.email = nextEmail;
        this.password = nextEncodedPassword;
        this.nickname = nextNickname;
        this.activityRegion = nextActivityRegion;
        this.updatedAt = LocalDateTime.now();

        if (regionChanged) {
            this.activityRegionVerified = false;
        }
    }

    public void delete(UUID deletedBy) {
        LocalDateTime now = LocalDateTime.now();
        if (this.status == MemberStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 회원입니다.");
        } else {
            this.status = MemberStatus.DELETED;
        }
        this.deletedAt = now;
        this.deletedBy = deletedBy;
        this.updatedAt = now;
        this.updatedBy = deletedBy;
    }

    private void validate(String email, String encodedPassword, String nickname, ActivityRegion activityRegion) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }

        if (email.length() > 255) {
            throw new IllegalArgumentException("이메일은 255자를 초과할 수 없습니다.");
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }

        if (encodedPassword.length() > 255) {
            throw new IllegalArgumentException("비밀번호는 255자를 초과할 수 없습니다.");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }

        if (nickname.length() > 50) {
            throw new IllegalArgumentException("닉네임은 50자를 초과할 수 없습니다.");
        }
        if (activityRegion == null) {
            throw new IllegalArgumentException("활동 지역은 필수입니다.");
        }
    }

}
