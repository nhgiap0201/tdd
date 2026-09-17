package com.roomsync.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public Room(UUID id, String name, int capacity) {
        this(id != null ? id.toString() : null, name, capacity, true);
    }

    public Room(String id, String name, int capacity) {
        this(id, name, capacity, true);
    }
}
