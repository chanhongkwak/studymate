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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "meeting")
public class Meeting {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(23, 0);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID studyId;

    @Column(nullable = false)
    private UUID branchId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = true, length = 5000)
    private String description;

    @Column(nullable = false)
    private long maxCostPerPerson;

    //예약,모임 시작 시간, 한국 시간 기준
    @Column(nullable = false)
    private LocalDateTime startAt;

    //예약 종료 시각, 마지막 10분의 정리,청소 시간을 포함
    @Column(nullable = false)
    private LocalDateTime endAt;

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


    public Meeting(UUID studyId, UUID branchId, String title, String description, long maxCostPerPerson,
                   LocalDateTime startAt, LocalDateTime endAt, UUID createdBy) {
        if (studyId == null) {
            throw new IllegalArgumentException("스터디ID는 필수입니다.");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("지점ID는 필수입니다.");
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
        if (maxCostPerPerson < 1) {
            throw new IllegalArgumentException("1인당 최대 부담액은 1포인트 이상이어야 합니다.");
        }
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        validateSchedule(startAt, endAt, now);

        if (createdBy == null) {
            throw new IllegalArgumentException("생성자 ID는 필수입니다.");
        }

        this.studyId = studyId;
        this.branchId = branchId;
        this.title = title;
        this.description = description;
        this.maxCostPerPerson = maxCostPerPerson;
        this.startAt = startAt;
        this.endAt = endAt;

        this.status = MeetingStatus.PROPOSED;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public void update(
            UUID branchId, String title, String description, Long maxCostPerPerson, LocalDateTime startAt,
            LocalDateTime endAt,
            UUID updatedBy
    ) {
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("설명은 최대 5000자까지 가능합니다.");
        }

        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);

        if (!this.startAt.isAfter(now)) {
            throw new ConflictException("이미 시작한 모임은 수정할 수 없습니다.");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }
        if (this.status == MeetingStatus.CANCELED) {
            throw new ConflictException("취소된 모임은 수정할 수 없습니다.");
        }
        UUID nextBranchId = branchId == null ? this.branchId : branchId;
        String nextTitle = title == null ? this.title : title;
        String nextDescription = description == null ? this.description : description;
        long nextMaxCostPerPerson = maxCostPerPerson == null ? this.maxCostPerPerson : maxCostPerPerson;

        LocalDateTime nextStartAt = startAt == null ? this.startAt : startAt;
        LocalDateTime nextEndAt = endAt == null ? this.endAt : endAt;

        if (nextMaxCostPerPerson < 1) {
            throw new IllegalArgumentException("1인당 최대 부담액은 1포인트 이상이어야 합니다.");
        }

        boolean reservationConditionsChanged =
                !this.branchId.equals(nextBranchId)
                        || !this.startAt.equals(nextStartAt)
                        || !this.endAt.equals(nextEndAt)
                        || this.maxCostPerPerson != nextMaxCostPerPerson;

        if (this.status == MeetingStatus.CONFIRMED
                && reservationConditionsChanged) {
            throw new ConflictException(
                    "예약이 확정된 모임의 지점, 시간, 최대 부담액은 직접 변경할 수 없습니다."
            );
        }

        if (nextTitle.length() > 100) {
            throw new IllegalArgumentException("제목은 최대 100자까지 가능합니다.");
        }
        if (nextTitle.isBlank()) {
            throw new IllegalArgumentException("제목은 공백이 허용되지 않습니다.");
        }

        validateSchedule(nextStartAt, nextEndAt, now);

        this.branchId = nextBranchId;
        this.title = nextTitle;
        this.description = nextDescription;
        this.maxCostPerPerson = nextMaxCostPerPerson;
        this.startAt = nextStartAt;
        this.endAt = nextEndAt;
        this.updatedBy = updatedBy;

        this.updatedAt = now;
    }

    public void confirm(UUID confirmedBy) {
        if (confirmedBy == null) {
            throw new IllegalArgumentException("확정자ID는 필수입니다.");
        }
        if (this.status != MeetingStatus.PROPOSED) {
            throw new ConflictException("제안 상태의 모임만 확정할 수 있습니다.");
        }

        this.status = MeetingStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now(KOREA_ZONE);
        this.updatedBy = confirmedBy;
    }

    public void cancel(UUID canceledBy) {
        cancel(canceledBy, LocalDateTime.now(KOREA_ZONE));
    }

    public void cancel(UUID canceledBy, LocalDateTime canceledAt) {
        if (canceledBy == null) {
            throw new IllegalArgumentException("취소자 ID는 필수입니다.");
        }
        if (canceledAt == null) {
            throw new IllegalArgumentException("취소 시각은 필수입니다.");
        }
        if (this.status == MeetingStatus.CANCELED) {
            throw new ConflictException("이미 취소된 일정입니다.");
        }
        if (!this.startAt.isAfter(canceledAt)) {
            throw new ConflictException("이미 시작된 일정입니다.");
        }

        if (this.status == MeetingStatus.CONFIRMED) {
            throw new ConflictException(
                    "예약이 확정된 모임은 예약 취소 절차를 통해 취소해야 합니다."
            );
        }

        this.status = MeetingStatus.CANCELED;
        this.updatedAt = canceledAt;
        this.updatedBy = canceledBy;
    }

    public void cancelByReservation(UUID canceledBy, LocalDateTime canceledAt) {
        if (canceledBy == null) {
            throw new IllegalArgumentException("취소자 ID는 필수입니다.");
        }
        if (canceledAt == null) {
            throw new IllegalArgumentException("취소 시각은 필수입니다.");
        }
        if (this.status != MeetingStatus.CONFIRMED) {
            throw new ConflictException(
                    "예약이 확정된 모임만 예약 취소 절차로 취소할 수 있습니다."
            );
        }
        if (!this.startAt.isAfter(canceledAt)) {
            throw new ConflictException("이미 시작된 일정입니다.");
        }

        this.status = MeetingStatus.CANCELED;
        this.updatedAt = canceledAt;
        this.updatedBy = canceledBy;
    }

    private static void validateSchedule(
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime now
    ) {
        if (startAt == null) {
            throw new IllegalArgumentException("시작 시각은 필수입니다.");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("종료 시각은 필수입니다.");
        }

        if (!startAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "시작 시각은 현재 시각보다 이후여야 합니다."
            );
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "종료 시각은 시작 시각보다 이후여야 합니다."
            );
        }

        if (!startAt.toLocalDate().equals(endAt.toLocalDate())) {
            throw new IllegalArgumentException(
                    "예약 시작과 종료는 같은 날짜여야 합니다."
            );
        }

        if (startAt.toLocalTime().isBefore(OPEN_TIME)
                || endAt.toLocalTime().isAfter(CLOSE_TIME)) {
            throw new IllegalArgumentException(
                    "예약은 한국 시간 09:00부터 23:00 사이에 시작하고 종료해야 합니다."
            );
        }

        if (!isOnTheHour(startAt)) {
            throw new IllegalArgumentException(
                    "예약 시작 시각은 정각이어야 합니다."
            );
        }

        if (!isOnTheHour(endAt)) {
            throw new IllegalArgumentException(
                    "예약은 1시간 단위여야 합니다."
            );
        }
    }

    private static boolean isOnTheHour(LocalDateTime dateTime) {
        return dateTime.getMinute() == 0
                && dateTime.getSecond() == 0
                && dateTime.getNano() == 0;
    }

    public LocalDateTime calculateSessionEndAt() {
        return this.endAt.minusMinutes(10);
    }

    public LocalDateTime calculateNormalCheckoutStartAt() {
        return this.endAt.minusMinutes(20);
    }
}
