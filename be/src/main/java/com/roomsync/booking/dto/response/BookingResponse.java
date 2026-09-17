package com.roomsync.booking.dto.response;

import com.roomsync.booking.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết lượt đặt phòng họp")
public class BookingResponse {
    @Schema(description = "Mã định danh lượt đặt phòng (ID)", example = "b-123e4567-e89b")
    private String id;

    @Schema(description = "Mã phòng họp", example = "ROOM_01")
    private String roomId;

    @Schema(description = "Mã người dùng đặt lịch", example = "USER_EMP_01")
    private String bookedByUserId;

    @Schema(description = "Tiêu đề cuộc họp", example = "Sprint Planning Team A")
    private String title;

    @Schema(description = "Thời gian bắt đầu (ISO-8601 UTC)", example = "2026-10-01T10:00:00Z")
    private String startTime;

    @Schema(description = "Thời gian kết thúc (ISO-8601 UTC)", example = "2026-10-01T11:00:00Z")
    private String endTime;

    @Schema(description = "Số người tham gia", example = "5")
    private int attendees;

    @Schema(description = "Trạng thái đặt phòng", example = "CONFIRMED")
    private BookingStatus status;

    @Schema(description = "Thời điểm khởi tạo (ISO-8601 UTC)")
    private String createdAt;

    @Schema(description = "Thời điểm cập nhật mới nhất (ISO-8601 UTC)")
    private String updatedAt;
}
