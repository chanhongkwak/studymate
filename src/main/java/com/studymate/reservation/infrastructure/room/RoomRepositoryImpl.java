package com.studymate.reservation.infrastructure.room;

import com.studymate.reservation.domain.room.Room;
import com.studymate.reservation.domain.room.RoomRepository;
import com.studymate.reservation.domain.room.RoomStatus;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoomRepositoryImpl implements RoomRepository {
    private final RoomJpaRepository roomJpaRepository;

    @Override
    public Room save(Room room) {
        return roomJpaRepository.save(room);
    }

    @Override
    public boolean existsByBranchIdAndName(UUID branchId, String name) {
        return roomJpaRepository.existsByBranchIdAndName(branchId, name);
    }

    @Override
    public Optional<Room> findById(UUID roomId) {
        return roomJpaRepository.findById(roomId);
    }

    @Override
    public boolean existsByBranchIdAndNameAndIdNot(UUID branchId, String normalizedName, UUID roomId) {
        return roomJpaRepository.existsByBranchIdAndNameAndIdNot(branchId, normalizedName, roomId);
    }

    @Override
    public Page<Room> findAllByBranchIdAndStatus(UUID branchId, RoomStatus roomStatus, Pageable pageable) {
        return roomJpaRepository.findAllByBranchIdAndStatus(branchId, roomStatus, pageable);
    }

    @Override
    public Page<Room> findAllByBranchId(UUID branchId, Pageable pageable) {
        return roomJpaRepository.findAllByBranchId(branchId, pageable);
    }

    @Override
    public Optional<Room> findByIdForUpdate(UUID roomId) {
        return roomJpaRepository.findByIdForUpdate(roomId);
    }
}
