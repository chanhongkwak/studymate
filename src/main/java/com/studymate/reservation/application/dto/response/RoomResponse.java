package com.studymate.reservation.application.dto.response;

import com.studymate.reservation.domain.room.Room;
import com.studymate.reservation.domain.room.RoomStatus;
import java.util.UUID;

public record RoomResponse(
        UUID roomId,
        UUID branchId,
        String name,
        int capacity,
        long hourlyRate,
        RoomStatus status
) {
    public static RoomResponse from(Room room){
        return new RoomResponse(
                room.getId(),
                room.getBranchId(),
                room.getName(),
                room.getCapacity(),
                room.getHourlyRate(),
                room.getStatus()
        );
    }
}
