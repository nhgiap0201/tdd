package com.roomsync.booking.service;

import com.roomsync.booking.dto.request.BookingRequestDTO;
import com.roomsync.booking.dto.response.BookingResponseDTO;
import com.roomsync.booking.entity.Booking;
import com.roomsync.booking.entity.Room;
import com.roomsync.booking.enums.BookingErrorCode;
import com.roomsync.booking.exception.BookingException;
import com.roomsync.booking.repository.BookingRepository;
import com.roomsync.booking.repository.RoomRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingServiceImpl bookingService; // Sẽ báo đỏ do chưa có implementation

    private final UUID roomId = UUID.randomUUID();
    private final Instant now = Instant.now().plus(1, ChronoUnit.HOURS); // Giả lập thời gian tương lai

    @Test
    @DisplayName("TC-BE-01: Đặt phòng thành công khi phòng trống và đủ sức chứa")
    void shouldCreateBooking_WhenRoomIsAvailableAndCapacityIsSufficient() {
        // Given
        BookingRequestDTO request = new BookingRequestDTO(roomId, now, now.plusSeconds(3600), 5, "Họp Team", "UserA");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        when(bookingRepository.hasOverlappingBooking(roomId, request.startTime(), request.endTime())).thenReturn(false);
        
        // When
        BookingResponseDTO response = bookingService.createBooking(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(bookingRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-BE-05: Ném lỗi CAPACITY_EXCEEDED khi số người vượt sức chứa")
    void shouldThrowException_WhenAttendeesExceedRoomCapacity() {
        // Given
        BookingRequestDTO request = new BookingRequestDTO(roomId, now, now.plusSeconds(3600), 12, "Họp All-hands", "UserB");
        Room mockRoom = new Room(roomId, "Phòng Beta", 10); // max 10
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(BookingException.class)
            .hasFieldOrPropertyWithValue("errorCode", BookingErrorCode.CAPACITY_EXCEEDED);
            
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BE-06: Ném lỗi OVERLAPPING_BOOKING khi trùng lịch lấn giờ")
    void shouldThrowException_WhenBookingOverlaps() {
        // Given
        BookingRequestDTO request = new BookingRequestDTO(roomId, now, now.plusSeconds(3600), 5, "Họp Sync", "UserC");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        // Mô phỏng Database phát hiện trùng lịch
        when(bookingRepository.hasOverlappingBooking(roomId, request.startTime(), request.endTime())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(BookingException.class)
            .hasFieldOrPropertyWithValue("errorCode", BookingErrorCode.OVERLAPPING_BOOKING);
            
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BE-07: Hợp lệ khi chạm đuôi - chạm đầu (Không trùng lịch)")
    void shouldAllowBooking_WhenTimeTouchesBounds() {
        // Given
        Instant existingEndTime = now; 
        Instant newStartTime = now;
        Instant newEndTime = now.plusSeconds(3600);
        
        // Lịch cũ kết thúc đúng lúc lịch mới bắt đầu (Hợp lệ)
        BookingRequestDTO request = new BookingRequestDTO(roomId, newStartTime, newEndTime, 5, "Họp Chạm Giờ", "UserD");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        // Database sẽ không báo trùng (hasOverlappingBooking = false) do dùng logic < và > chứ không phải <= và >=
        when(bookingRepository.hasOverlappingBooking(roomId, request.startTime(), request.endTime())).thenReturn(false);

        // When
        BookingResponseDTO response = bookingService.createBooking(request);

        // Then
        assertThat(response).isNotNull();
        verify(bookingRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-BE-09: Ném lỗi ROOM_NOT_FOUND khi phòng không tồn tại")
    void shouldThrowException_WhenRoomDoesNotExist() {
        // Given
        BookingRequestDTO request = new BookingRequestDTO(roomId, now, now.plusSeconds(3600), 5, "Họp Ảo", "UserE");
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(BookingException.class)
            .hasFieldOrPropertyWithValue("errorCode", BookingErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("TC-BE-08: Mô phỏng Race Condition - 2 Request đồng thời chỉ 1 thành công")
    void shouldHandleRaceCondition_WhenConcurrentRequests() throws InterruptedException {
        // Given
        BookingRequestDTO request = new BookingRequestDTO(roomId, now, now.plusSeconds(3600), 5, "Họp Race", "UserF");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        
        // Mô phỏng lock hoặc behavior DB (Thread đầu tiên pass, Thread sau vướng DataIntegrityViolationException)
        // Trong Unit Test tầng Service, ta giả lập logic này thông qua Repository (Ném lỗi khi lưu bị trùng db constraint)
        when(bookingRepository.save(any()))
            .thenReturn(new Booking()) // Request 1 thành công
            .thenThrow(new RuntimeException("DataIntegrityViolationException: Khóa Exclusion Constraint PostgreSQL")); // Request 2 thất bại
        
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    bookingService.createBooking(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // Then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-BE-INVAR-01: Bất biến 2 - Ném lỗi DURATION_INVALID khi thời lượng cuộc họp vượt quá 2 giờ (120 phút)")
    void shouldThrowException_WhenDurationExceedsTwoHours() {
        // Given: Cuộc họp 180 phút (3 giờ) vào Thứ 2 tuần tới lúc 09:00 - 12:00
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Instant start = nextMonday.atTime(9, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant end = nextMonday.atTime(12, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

        BookingRequestDTO request = new BookingRequestDTO(roomId, start, end, 5, "Họp Chiến Lược Dài", "UserTechLead");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(BookingException.class)
            .hasFieldOrPropertyWithValue("errorCode", BookingErrorCode.DURATION_INVALID);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BE-INVAR-02: Bất biến 2 - Ném lỗi DURATION_INVALID khi thời lượng cuộc họp dưới mức tối thiểu 15 phút")
    void shouldThrowException_WhenDurationIsLessThanFifteenMinutes() {
        // Given: Cuộc họp 10 phút vào Thứ 2 tuần tới lúc 09:00 - 09:10
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Instant start = nextMonday.atTime(9, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant end = nextMonday.atTime(9, 10).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

        BookingRequestDTO request = new BookingRequestDTO(roomId, start, end, 5, "Họp Nhanh 10 Phút", "UserTechLead");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(BookingException.class)
            .hasFieldOrPropertyWithValue("errorCode", BookingErrorCode.DURATION_INVALID);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BE-INVAR-03: Bất biến 3 - Ném lỗi OUTSIDE_BUSINESS_HOURS khi đặt ngoài khung giờ 08:00 - 18:00")
    void shouldThrowException_WhenBookingIsOutsideBusinessHours() {
        // Given: Đặt phòng lúc 19:00 - 20:00 (ngoài giờ hành chính)
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Instant start = nextMonday.atTime(19, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant end = nextMonday.atTime(20, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

        BookingRequestDTO request = new BookingRequestDTO(roomId, start, end, 5, "Họp Tăng Ca Đêm", "UserTechLead");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(BookingException.class)
            .hasFieldOrPropertyWithValue("errorCode", BookingErrorCode.OUTSIDE_BUSINESS_HOURS);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BE-INVAR-04: Bất biến 3 - Ném lỗi WEEKEND_NOT_ALLOWED khi đặt vào Thứ Bảy hoặc Chủ Nhật")
    void shouldThrowException_WhenBookingIsOnWeekend() {
        // Given: Đặt phòng vào Thứ Bảy lúc 10:00 - 11:00
        LocalDate nextSaturday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        Instant start = nextSaturday.atTime(10, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant end = nextSaturday.atTime(11, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

        BookingRequestDTO request = new BookingRequestDTO(roomId, start, end, 5, "Họp Cuối Tuần Thứ Bảy", "UserTechLead");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(BookingException.class)
            .hasFieldOrPropertyWithValue("errorCode", BookingErrorCode.WEEKEND_NOT_ALLOWED);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BE-TIME-01: Bất biến thời gian - Ném lỗi PAST_TIME_INVALID khi thời gian bắt đầu ở quá khứ")
    void shouldThrowException_WhenStartTimeIsInThePast() {
        // Given: Thời gian bắt đầu 2 giờ trước
        Instant pastStart = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant pastEnd = pastStart.plus(1, ChronoUnit.HOURS);

        BookingRequestDTO request = new BookingRequestDTO(roomId, pastStart, pastEnd, 5, "Họp Ngược Thời Gian", "UserTechLead");
        Room mockRoom = new Room(roomId, "Phòng Alpha", 10);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
            .isInstanceOf(BookingException.class)
            .hasFieldOrPropertyWithValue("errorCode", BookingErrorCode.PAST_TIME_INVALID);

        verify(bookingRepository, never()).save(any());
    }
}
