package com.roomsync.booking.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public record BookingRequestDTO(
    @NotNull(message = "Room ID is required")
    UUID roomId,

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    Instant startTime,

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    Instant endTime,

    @NotNull(message = "Number of attendees is required")
    @Positive(message = "Attendees must be greater than zero")
    Integer attendees,

    @NotNull(message = "Title is required")
    String title,

    @NotNull(message = "Booked by is required")
    String bookedBy
) {}
