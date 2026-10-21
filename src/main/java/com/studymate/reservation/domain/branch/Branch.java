package com.studymate.reservation.domain.branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "branches")
public class Branch {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BranchStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;

    public Branch(
            String name, String address, UUID createdBy
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("이름은 100자를 넘을 수 없습니다.");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }
        if (address.length() > 255) {
            throw new IllegalArgumentException("주소는 255자를 넘을 수 없습니다.");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("생성자 ID는 필수입니다.");
        }

        this.name = name;
        this.address = address;
        this.status = BranchStatus.ACTIVE;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.createdAt = now;
        this.createdBy = createdBy;
        this.updatedAt = now;
        this.updatedBy = createdBy;
    }

    public void update(
            String name, String address, UUID updatedBy
    ) {
        if (name == null && address == null) {
            throw new IllegalArgumentException("수정되는 곳이 없습니다.");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자 ID는 필수입니다.");
        }

        String nextName = name == null ? this.name : name;
        String nextAddress = address == null ? this.address : address;

        if (nextName == null || nextName.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (nextName.length() > 100) {
            throw new IllegalArgumentException("이름은 100자를 넘을 수 없습니다.");
        }
        if (nextAddress == null || nextAddress.isBlank()) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }
        if (nextAddress.length() > 255) {
            throw new IllegalArgumentException("주소는 255자를 넘을 수 없습니다.");
        }

        if (this.name.equals(nextName) && this.address.equals(nextAddress)) {
            return;
        }

        this.name = nextName;
        this.address = nextAddress;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    public void changeStatus(
            BranchStatus status, UUID updatedBy
    ) {
        if (status == null) {
            throw new IllegalArgumentException("상태 변경은 필수입니다.");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자 ID는 필수입니다.");
        }

        if (this.status == status) {
            return;
        }

        this.status = status;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }
}
