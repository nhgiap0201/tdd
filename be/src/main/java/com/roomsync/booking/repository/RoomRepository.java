package com.roomsync.booking.repository;

import com.roomsync.booking.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    default Optional<Room> findById(UUID id) {
        return id == null ? Optional.empty() : findById(id.toString());
    }
}
