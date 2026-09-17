package com.roomsync.booking.service;

import com.roomsync.booking.dto.request.BookingRequestDTO;
import com.roomsync.booking.dto.request.CreateBookingRequest;
import com.roomsync.booking.dto.response.BookingResponse;
import com.roomsync.booking.dto.response.BookingResponseDTO;
import com.roomsync.booking.dto.response.RoomResponse;

import java.util.List;

public interface BookingService {
    BookingResponseDTO createBooking(BookingRequestDTO request);
    BookingResponse createBooking(String userId, CreateBookingRequest request, String idempotencyKey);
    List<RoomResponse> getRooms();
    List<BookingResponse> getBookings();
}
