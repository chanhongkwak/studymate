package com.studymate.reservation.domain.room;

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
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rooms_branch_id_name",
                columnNames = {"branch_id", "name"}
        )
)
public class Room {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private long hourlyRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;

    public Room(
            UUID branchId,
            String name,
            int capacity,
            long hourlyRate,
            UUID createdBy
    ) {
        if (branchId == null) {
            throw new IllegalArgumentException("지점ID는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }

        String normalizedName = name.strip();

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException("이름은 100자까지 허용합니다.");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("최대 이용인원은 1명 이상이여야 합니다.");
        }
        if (hourlyRate < 1) {
            throw new IllegalArgumentException("시간당 요금은 1포인트 이상이어야 합니다.");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("생성자ID는 필수입니다.");
        }

        this.branchId = branchId;
        this.name = normalizedName;
        this.capacity = capacity;
        this.hourlyRate = hourlyRate;
        this.status = RoomStatus.ACTIVE;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.createdAt = now;
        this.createdBy = createdBy;
        this.updatedAt = now;
        this.updatedBy = createdBy;
    }

    public void update(
            String name,
            Integer capacity,
            Long hourlyRate,
            UUID updatedBy
    ) {
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }
        if (name == null && capacity == null && hourlyRate == null) {
            throw new IllegalArgumentException("수정할 항목이 없습니다.");
        }

        String nextName = name == null ? this.name : name.strip();
        int nextCapacity = capacity == null ? this.capacity : capacity;
        long nextHourlyRate = hourlyRate == null ? this.hourlyRate : hourlyRate;

        if (nextName.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (nextName.length() > 100) {
            throw new IllegalArgumentException("이름은 100자까지 허용합니다.");
        }
        if (nextCapacity < 1) {
            throw new IllegalArgumentException("최대 이용인원은 1명 이상이여야 합니다.");
        }
        if (nextHourlyRate < 1) {
            throw new IllegalArgumentException("시간당 요금은 1포인트 이상이어야 합니다.");
        }

        if (this.name.equals(nextName)
                && this.capacity == nextCapacity
                && this.hourlyRate == nextHourlyRate) {
            return;
        }

        this.name = nextName;
        this.capacity = nextCapacity;
        this.hourlyRate = nextHourlyRate;

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    public void changeStatus(
            RoomStatus status,
            UUID updatedBy
    ) {
        if (status == null) {
            throw new IllegalArgumentException("상태변경은 필수입니다.");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자는 필수입니다.");
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
