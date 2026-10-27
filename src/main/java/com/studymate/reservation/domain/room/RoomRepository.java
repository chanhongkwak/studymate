package com.studymate.reservation.domain.room;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomRepository {
    Room save(Room room);

    boolean existsByBranchIdAndName(UUID branchId, String name);

    Optional<Room> findById(UUID roomId);

    boolean existsByBranchIdAndNameAndIdNot(UUID branchId, String normalizedName, UUID roomId);

    Page<Room> findAllByBranchIdAndStatus(UUID branchId, RoomStatus roomStatus, Pageable pageable);

    Page<Room> findAllByBranchId(UUID branchId, Pageable pageable);

    Optional<Room> findByIdForUpdate(UUID roomId);
}
