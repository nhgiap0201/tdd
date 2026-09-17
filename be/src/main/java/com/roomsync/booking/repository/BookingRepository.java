package com.roomsync.booking.repository;

import com.roomsync.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId AND b.status <> 'CANCELLED' " +
           "AND (b.startTime < :endTime AND b.endTime > :startTime)")
    List<Booking> findOverlapping(
        @Param("roomId") String roomId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    default boolean hasOverlappingBooking(UUID roomId, Instant startTime, Instant endTime) {
        if (roomId == null || startTime == null || endTime == null) {
            return false;
        }
        return !findOverlapping(roomId.toString(), startTime, endTime).isEmpty();
    }

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    default void saveIdempotency(String key, Booking booking) {
        if (booking != null) {
            booking.setIdempotencyKey(key);
            save(booking);
        }
    }
}
