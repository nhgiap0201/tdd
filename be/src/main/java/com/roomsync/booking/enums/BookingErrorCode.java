package com.roomsync.booking.enums;

import org.springframework.http.HttpStatus;

public enum BookingErrorCode {
    OVERLAPPING_BOOKING(HttpStatus.CONFLICT, "Phòng đã có người đặt trong khung giờ này!"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Phòng họp không tồn tại!"),
    ROOM_INACTIVE(HttpStatus.BAD_REQUEST, "Phòng họp đang ở trạng thái bảo trì, không thể đặt!"),
    CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "Số lượng người tham gia vượt quá sức chứa tối đa của phòng!"),
    EXCEEDS_CAPACITY(HttpStatus.BAD_REQUEST, "Số lượng người tham gia vượt quá sức chứa tối đa của phòng!"),
    PAST_TIME(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu cuộc họp phải ở thời điểm tương lai!"),
    PAST_TIME_INVALID(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu cuộc họp phải ở thời điểm tương lai!"),
    DURATION_INVALID(HttpStatus.BAD_REQUEST, "Thời lượng cuộc họp không hợp lệ (phải từ 15 đến 120 phút)!"),
    OUTSIDE_BUSINESS_HOURS(HttpStatus.BAD_REQUEST, "Chỉ được đặt phòng trong khung giờ hành chính (08:00 - 18:00)!"),
    WEEKEND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Không thể đặt phòng vào Thứ Bảy hoặc Chủ Nhật!"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ!"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống nội bộ!");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    BookingErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
