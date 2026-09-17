package com.roomsync.booking.entity;

import com.roomsync.booking.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_room_time", columnList = "room_id, start_time, end_time"),
        @Index(name = "idx_booking_idempotency", columnList = "idempotency_key")
    }
)
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "room_id", nullable = false, length = 64)
    private String roomId;

    @Column(name = "booked_by_user_id", length = 100)
    private String bookedByUserId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "attendees", nullable = false)
    private int attendees;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BookingStatus status;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = BookingStatus.CONFIRMED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
