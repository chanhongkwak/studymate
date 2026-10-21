package com.studymate.reservation.infrastructure.room;

import com.studymate.reservation.domain.room.Room;
import com.studymate.reservation.domain.room.RoomStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<Room, UUID> {

    boolean existsByBranchIdAndName(UUID branchId, String name);

    Optional<Room> findById(UUID roomId);

    boolean existsByBranchIdAndNameAndIdNot(UUID branchId, String normalizedName, UUID roomId);

    Page<Room> findAllByBranchIdAndStatus(UUID branchId, RoomStatus roomStatus, Pageable pageable);

    Page<Room> findAllByBranchId(UUID branchId, Pageable pageable);
}
