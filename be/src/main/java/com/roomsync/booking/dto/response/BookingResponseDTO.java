package com.roomsync.booking.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BookingResponseDTO(
    UUID id,
    UUID roomId,
    Instant startTime,
    Instant endTime,
    Integer attendees,
    String title,
    String bookedBy,
    String status
) {}
