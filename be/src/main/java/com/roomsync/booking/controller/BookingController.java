package com.roomsync.booking.controller;

import com.roomsync.booking.dto.request.CreateBookingRequest;
import com.roomsync.booking.dto.response.BookingResponse;
import com.roomsync.booking.dto.response.RoomResponse;
import com.roomsync.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "Room & Booking Management", description = "API quản lý phòng họp và đặt lịch tuân thủ 3 Invariants")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/api/health")
    @Operation(summary = "Kiểm tra trạng thái dịch vụ (Health Check)", description = "Trả về trạng thái hoạt động của hệ thống Spring Boot API")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Meeting Room Booking Spring Boot API"));
    }

    @GetMapping("/api/v1/rooms")
    @Operation(summary = "Lấy danh sách phòng họp", description = "Lấy toàn bộ danh sách phòng họp trong hệ thống, bao gồm phòng hoạt động và bảo trì")
    @ApiResponse(responseCode = "200", description = "Tải danh sách phòng thành công")
    public ResponseEntity<List<RoomResponse>> getRooms() {
        return ResponseEntity.ok(bookingService.getRooms());
    }

    @GetMapping("/api/v1/bookings")
    @Operation(summary = "Lấy danh sách lịch đặt phòng", description = "Lấy danh sách tất cả các lịch đặt phòng họp đã được ghi nhận")
    @ApiResponse(responseCode = "200", description = "Tải danh sách lịch đặt phòng thành công")
    public ResponseEntity<List<BookingResponse>> getBookings() {
        return ResponseEntity.ok(bookingService.getBookings());
    }

    @PostMapping("/api/v1/bookings")
    @Operation(summary = "Tạo mới lịch đặt phòng", description = "Kiểm tra và đặt phòng họp mới. Tự động xác thực 3 Bất biến (NO_OVERLAP, MAX_2_HOURS, BUSINESS_HOURS_ONLY)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đặt phòng thành công (CONFIRMED)"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (sai giờ, quá sức chứa, cuối tuần, quá 2h)"),
        @ApiResponse(responseCode = "404", description = "Phòng họp không tồn tại"),
        @ApiResponse(responseCode = "409", description = "Trùng lịch đặt phòng (OVERLAPPING_BOOKING)")
    })
    public ResponseEntity<BookingResponse> createBooking(
            @Parameter(description = "Idempotency key chống trùng lặp request khi mạng chập chờn")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(description = "ID người dùng nhân viên đặt phòng")
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "USER_EMP_01") String userId,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        BookingResponse response = bookingService.createBooking(userId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
