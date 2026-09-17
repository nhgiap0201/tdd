package com.roomsync.booking.service;

import com.roomsync.booking.dto.request.BookingRequestDTO;
import com.roomsync.booking.dto.request.CreateBookingRequest;
import com.roomsync.booking.dto.response.BookingResponse;
import com.roomsync.booking.dto.response.BookingResponseDTO;
import com.roomsync.booking.dto.response.RoomResponse;
import com.roomsync.booking.entity.Booking;
import com.roomsync.booking.entity.Room;
import com.roomsync.booking.enums.BookingErrorCode;
import com.roomsync.booking.enums.BookingStatus;
import com.roomsync.booking.exception.BookingException;
import com.roomsync.booking.constant.BookingLimits;
import com.roomsync.booking.repository.BookingRepository;
import com.roomsync.booking.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    private final Map<String, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public BookingServiceImpl(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this(roomRepository, bookingRepository, Clock.systemUTC());
    }

    public BookingServiceImpl(RoomRepository roomRepository, BookingRepository bookingRepository, Clock clock) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    private ReentrantLock getRoomLock(String roomId) {
        return roomLocks.computeIfAbsent(roomId == null ? "" : roomId, k -> new ReentrantLock());
    }

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        // 1. Kiểm tra phòng tồn tại
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new BookingException(
                        BookingErrorCode.ROOM_NOT_FOUND,
                        "Phòng họp không tồn tại"
                ));

        // 2. Kiểm tra sức chứa
        if (request.attendees() != null && request.attendees() > room.getCapacity()) {
            throw new BookingException(
                    BookingErrorCode.CAPACITY_EXCEEDED,
                    "Số người tham gia vượt quá sức chứa tối đa của phòng"
            );
        }

        // 3. Kiểm tra thời gian bắt đầu trong quá khứ
        Instant current = (clock != null) ? clock.instant() : Instant.now();
        if (request.startTime().isBefore(current)) {
            throw new BookingException(
                    BookingErrorCode.PAST_TIME_INVALID,
                    "Thời gian bắt đầu không được nằm trong quá khứ"
            );
        }

        // 4. Bất biến 2 (MAX_2_HOURS & MIN_15_MINUTES): Kiểm tra thời lượng cuộc họp
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BookingException(
                    BookingErrorCode.DURATION_INVALID,
                    "Thời gian kết thúc phải lớn hơn thời gian bắt đầu"
            );
        }
        long durationMinutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
        if (durationMinutes < 15 || durationMinutes > 120) {
            throw new BookingException(
                    BookingErrorCode.DURATION_INVALID,
                    "Thời lượng cuộc họp phải từ 15 phút đến 120 phút"
            );
        }

        // 5. Bất biến 3 (BUSINESS_HOURS_ONLY): Kiểm tra ngày trong tuần và khung giờ 08:00 - 18:00
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime startZoned = request.startTime().atZone(zone);
        ZonedDateTime endZoned = request.endTime().atZone(zone);

        DayOfWeek dayOfWeek = startZoned.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            throw new BookingException(
                    BookingErrorCode.WEEKEND_NOT_ALLOWED,
                    "Không được phép đặt phòng vào Thứ Bảy hoặc Chủ Nhật"
            );
        }

        LocalTime startTime = startZoned.toLocalTime();
        LocalTime endTime = endZoned.toLocalTime();
        if (startTime.isBefore(LocalTime.of(8, 0)) || startTime.isAfter(LocalTime.of(18, 0))) {
            throw new BookingException(
                    BookingErrorCode.OUTSIDE_BUSINESS_HOURS,
                    "Chỉ được đặt phòng trong khung giờ hành chính từ 08:00 đến 18:00"
            );
        }

        // 6. Bất biến 1 (NO_OVERLAP): Kiểm tra trùng lịch: Math.max(startA, startB) < Math.min(endA, endB)
        if (bookingRepository.hasOverlappingBooking(request.roomId(), request.startTime(), request.endTime())) {
            throw new BookingException(
                    BookingErrorCode.OVERLAPPING_BOOKING,
                    "Phòng họp đã có người đặt trong khung giờ này"
            );
        }

        // 4. Lưu thông tin đặt phòng
        Booking booking = Booking.builder()
                .roomId(request.roomId().toString())
                .bookedByUserId(request.bookedBy())
                .title(request.title())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .attendees(request.attendees())
                .status(BookingStatus.CONFIRMED)
                .createdAt(Instant.now())
                .build();

        bookingRepository.save(booking);

        // 5. Trả về response DTO
        return new BookingResponseDTO(
                UUID.randomUUID(),
                request.roomId(),
                request.startTime(),
                request.endTime(),
                request.attendees(),
                request.title(),
                request.bookedBy(),
                "CONFIRMED"
        );
    }

    /**
     * Thuật toán kiểm tra trùng lịch giữa 2 khoảng thời gian [startA, endA) và [startB, endB):
     * Math.max(startA, startB) < Math.min(endA, endB)
     */
    public static boolean isOverlapping(Instant startA, Instant endA, Instant startB, Instant endB) {
        return Math.max(startA.toEpochMilli(), startB.toEpochMilli()) < Math.min(endA.toEpochMilli(), endB.toEpochMilli());
    }

    @Override
    public BookingResponse createBooking(String userId, CreateBookingRequest request, String idempotencyKey) {
        ReentrantLock lock = getRoomLock(request.getRoomId());
        lock.lock();
        try {
            // 1. Kiểm tra Idempotency Key
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                Optional<Booking> existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    return toBookingResponse(existing.get());
                }
            }

            // 2. Validate thời gian
            Instant startInstant;
            Instant endInstant;
            try {
                startInstant = Instant.parse(request.getStartTime());
                endInstant = Instant.parse(request.getEndTime());
            } catch (DateTimeParseException | NullPointerException e) {
                throw new BookingException(
                        BookingErrorCode.DURATION_INVALID,
                        422,
                        "Thời gian bắt đầu hoặc kết thúc không hợp lệ"
                );
            }

            Instant now = clock.instant();

            // So sánh nghiêm ngặt: startInstant < now
            if (startInstant.isBefore(now)) {
                throw new BookingException(
                        BookingErrorCode.PAST_TIME_INVALID,
                        422,
                        "Thời gian bắt đầu không được nằm trong quá khứ"
                );
            }

            if (!endInstant.isAfter(startInstant)) {
                throw new BookingException(
                        BookingErrorCode.DURATION_INVALID,
                        422,
                        "Thời gian kết thúc phải lớn hơn thời gian bắt đầu"
                );
            }

            long durationMinutes = Duration.between(startInstant, endInstant).toMinutes();
            if (durationMinutes < BookingLimits.MIN_DURATION_MINUTES ||
                    durationMinutes > BookingLimits.MAX_DURATION_HOURS * 60) {
                throw new BookingException(
                        BookingErrorCode.DURATION_INVALID,
                        422,
                        String.format("Thời lượng cuộc họp phải từ %d phút đến %d giờ",
                                BookingLimits.MIN_DURATION_MINUTES, BookingLimits.MAX_DURATION_HOURS)
                );
            }

            // 3. Validate phòng họp
            Optional<Room> roomOpt = roomRepository.findById(request.getRoomId());
            if (roomOpt.isEmpty() || !roomOpt.get().isActive()) {
                throw new BookingException(
                        BookingErrorCode.ROOM_NOT_FOUND,
                        404,
                        "Phòng họp không tồn tại hoặc đã ngừng hoạt động"
                );
            }
            Room room = roomOpt.get();

            // 4. Validate sức chứa và số người tham gia
            if (request.getAttendees() == null ||
                    request.getAttendees() < BookingLimits.MIN_ATTENDEES ||
                    request.getAttendees() > room.getCapacity()) {
                throw new BookingException(
                        BookingErrorCode.EXCEEDS_CAPACITY,
                        422,
                        String.format("Số người tham gia phải từ %d đến sức chứa tối đa của phòng (%d)",
                                BookingLimits.MIN_ATTENDEES, room.getCapacity())
                );
            }

            // 5. Kiểm tra trùng lịch (Overlapping Check)
            List<Booking> repoOverlaps = bookingRepository.findOverlapping(request.getRoomId(), startInstant, endInstant);
            if (repoOverlaps != null && !repoOverlaps.isEmpty()) {
                throw new BookingException(
                        BookingErrorCode.OVERLAPPING_BOOKING,
                        409,
                        "Phòng họp đã có người đặt trong khung giờ này"
                );
            }

            // 6. Lưu bản ghi đặt phòng mới (Không gán id thủ công, để JPA sinh UUID tự động)
            Booking newBooking = Booking.builder()
                    .roomId(request.getRoomId())
                    .bookedByUserId(userId)
                    .title(request.getTitle())
                    .startTime(startInstant)
                    .endTime(endInstant)
                    .attendees(request.getAttendees())
                    .status(BookingStatus.CONFIRMED)
                    .createdAt(now)
                    .build();

            Booking savedBooking = bookingRepository.save(newBooking);

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                bookingRepository.saveIdempotency(idempotencyKey, savedBooking);
            }

            return toBookingResponse(savedBooking);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<RoomResponse> getRooms() {
        return roomRepository.findAll().stream()
                .map(r -> RoomResponse.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .capacity(r.getCapacity())
                        .isActive(r.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponse> getBookings() {
        return bookingRepository.findAll().stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .roomId(booking.getRoomId())
                .bookedByUserId(booking.getBookedByUserId())
                .title(booking.getTitle())
                .startTime(booking.getStartTime().toString())
                .endTime(booking.getEndTime().toString())
                .attendees(booking.getAttendees())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : null)
                .updatedAt(booking.getUpdatedAt() != null ? booking.getUpdatedAt().toString() : null)
                .build();
    }
}
