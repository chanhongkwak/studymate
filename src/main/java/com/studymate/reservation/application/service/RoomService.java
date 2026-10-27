package com.studymate.reservation.application.service;

import com.studymate.global.exception.ForbiddenException;
import com.studymate.global.exception.NotFoundException;
import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import com.studymate.member.domain.MemberRole;
import com.studymate.member.domain.MemberStatus;
import com.studymate.reservation.application.dto.request.RoomCreateRequest;
import com.studymate.reservation.application.dto.request.RoomUpdateRequest;
import com.studymate.reservation.application.dto.response.RoomResponse;
import com.studymate.reservation.domain.branch.Branch;
import com.studymate.reservation.domain.branch.BranchRepository;
import com.studymate.reservation.domain.branch.BranchStatus;
import com.studymate.reservation.domain.room.Room;
import com.studymate.reservation.domain.room.RoomRepository;
import com.studymate.reservation.domain.room.RoomStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final BranchRepository branchRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public RoomResponse create(RoomCreateRequest request, UUID createdBy) {
        if (createdBy == null) {
            throw new IllegalArgumentException("생성자ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(createdBy)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 회원만 방을 관리할 수 있습니다.");
        }
        if (member.getRole() != MemberRole.MASTER) {
            throw new ForbiddenException("관리자만 방을 생성할 수 있습니다.");
        }

        if (request.capacity() == null) {
            throw new IllegalArgumentException("최대 이용 인원은 필수입니다.");
        }
        if (request.hourlyRate() == null) {
            throw new IllegalArgumentException("시간당 요금은 필수입니다.");
        }

        if (request.branchId() == null) {
            throw new IllegalArgumentException("지점 ID는 필수입니다.");
        }
        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new NotFoundException("해당 지점을 찾을 수 없습니다."));

        Room room = new Room(
                branch.getId(),
                request.name(),
                request.capacity(),
                request.hourlyRate(),
                createdBy
        );

        if (roomRepository.existsByBranchIdAndName(room.getBranchId(), room.getName())) {
            throw new IllegalArgumentException("중복된 이름의 방이 존재합니다.");
        }

        roomRepository.save(room);

        return RoomResponse.from(room);
    }

    @Transactional
    public RoomResponse update(RoomUpdateRequest request, UUID roomId, UUID updateBy) {
        if (updateBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }
        Member member = memberRepository.findByIdAndDeletedAtIsNull(updateBy)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 상태의 회원만 수정 할 수 있습니다.");
        }
        if (member.getRole() != MemberRole.MASTER) {
            throw new ForbiddenException("마스터 권한만 수정 할 수 있습니다.");
        }

        if (roomId == null) {
            throw new IllegalArgumentException("룸ID는 필수입니다.");
        }
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() ->
                        new NotFoundException("해당 방을 찾을 수 없습니다."));

        if (request.name() != null) {
            String normalizedName = request.name().strip();

            if (roomRepository.existsByBranchIdAndNameAndIdNot(
                    room.getBranchId(),
                    normalizedName,
                    room.getId()
            )) {
                throw new IllegalArgumentException("중복된 이름의 방이 존재합니다.");
            }
        }

        room.update(
                request.name(),
                request.capacity(),
                request.hourlyRate(),
                updateBy
        );

        return RoomResponse.from(room);
    }

    @Transactional
    public RoomResponse changeStatus(UUID roomId, RoomStatus status, UUID updatedBy) {

        if (updatedBy == null) {
            throw new IllegalArgumentException("수정자ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(updatedBy)
                .orElseThrow(() -> new NotFoundException("해당 회원을 찾을 수 없습니다."));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("활성 상태의 회원이 아닙니다.");
        }
        if (member.getRole() != MemberRole.MASTER) {
            throw new ForbiddenException("마스터 권한이 아닙니다.");
        }

        if (roomId == null) {
            throw new IllegalArgumentException("방ID는 필수입니다.");
        }
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() ->
                        new NotFoundException("해당 방을 찾을 수 없습니다."));

        room.changeStatus(status, updatedBy);

        return RoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(UUID roomId){
        if(roomId == null){
            throw new IllegalArgumentException("룸ID는 필수입니다.");
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("해당 룸을 찾을 수 없습니다."));

        return RoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getRooms(
            UUID branchId, int page, int size
    ){
        if(branchId == null){
            throw new IllegalArgumentException("지점 ID는 필수입니다.");
        }
        if(page<0){
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if(size<1 || size >100){
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("해당 지점을 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")
                )
        );

        if(branch.getStatus() != BranchStatus.ACTIVE)
        {
            return Page.empty(pageable);
        }

        Page<Room> rooms = roomRepository.findAllByBranchIdAndStatus(
                branchId,
                RoomStatus.ACTIVE,
                pageable
                );

        return rooms.map(RoomResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getRoomsForAdmin(
            UUID memberId,
            UUID branchId,
            RoomStatus status,
            int page,
            int size
    ){
        if(memberId == null){
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if(member.getStatus() != MemberStatus.ACTIVE){
            throw new ForbiddenException("활성 회원만 조회할 수 있습니다.");
        }
        if (member.getRole() != MemberRole.MASTER) {
            throw new ForbiddenException("관리자만 조회할 수 있습니다.");
        }

        if (branchId == null) {
            throw new IllegalArgumentException("지점 ID는 필수입니다.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }

        branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("해당 지점을 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")
                )
        );

        Page<Room> rooms;

        if (status == null) {
            rooms = roomRepository.findAllByBranchId(branchId, pageable);
        } else {
            rooms = roomRepository.findAllByBranchIdAndStatus(
                    branchId,
                    status,
                    pageable
            );
        }

        return rooms.map(RoomResponse::from);
    }
}
