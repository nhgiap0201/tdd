package com.roomsync.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tạo lịch đặt phòng họp mới")
public class CreateBookingRequest {
    @NotNull(message = "Mã phòng (roomId) không được để trống")
    @Schema(description = "Mã định danh phòng họp", example = "ROOM_01")
    private String roomId;

    @Schema(description = "Tiêu đề cuộc họp", example = "Sprint Planning Team A")
    private String title;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @Schema(description = "Thời gian bắt đầu (chuẩn ISO-8601 UTC)", example = "2026-10-01T10:00:00.000Z")
    private String startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @Schema(description = "Thời gian kết thúc (chuẩn ISO-8601 UTC)", example = "2026-10-01T11:00:00.000Z")
    private String endTime;

    @Schema(description = "Số lượng người tham dự cuộc họp", example = "5")
    private Integer attendees;
}
