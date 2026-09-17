package com.roomsync.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Chuẩn phản hồi chung từ API (Standard API Response Wrapper)")
public class ApiResponse<T> {

    @Schema(description = "Mã trạng thái phản hồi HTTP (status)", example = "200")
    private int status;

    @Schema(description = "Mã trạng thái phản hồi HTTP (code)", example = "200")
    private int code;

    @Schema(description = "Mã lỗi ngắn gọn (nếu có)", example = "OVERLAPPING_BOOKING")
    private String error;

    @Schema(description = "Mã định danh lỗi nghiệp vụ (Enum Code)", example = "OVERLAPPING_BOOKING")
    private String errorCode;

    @Schema(description = "Thông điệp phản hồi", example = "Thành công")
    private String message;

    @Schema(description = "Dữ liệu trả về (Generic Data Payload)")
    private T data;

    @Schema(description = "Đường dẫn request (URI)", example = "/api/v1/bookings")
    private String path;

    @Builder.Default
    @Schema(description = "Thời gian tạo phản hồi (ISO-8601 UTC)", example = "2026-09-17T14:30:00.000Z")
    private String timestamp = Instant.now().toString();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .code(200)
                .message("Thành công")
                .data(data)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .code(status)
                .message(message)
                .data(data)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String errorCode, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .code(status)
                .error(errorCode)
                .errorCode(errorCode)
                .message(message)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String errorCode, String message, String path) {
        return ApiResponse.<T>builder()
                .status(status)
                .code(status)
                .error(errorCode)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .timestamp(Instant.now().toString())
                .build();
    }
}
