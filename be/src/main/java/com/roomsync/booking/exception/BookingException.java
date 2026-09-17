package com.roomsync.booking.exception;

import com.roomsync.booking.enums.BookingErrorCode;
import lombok.Getter;

@Getter
public class BookingException extends RuntimeException {
    private final BookingErrorCode errorCode;
    private final int statusCode;

    public BookingException(BookingErrorCode errorCode) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Booking error");
        this.errorCode = errorCode;
        this.statusCode = errorCode != null ? errorCode.getHttpStatus().value() : 400;
    }

    public BookingException(BookingErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = errorCode != null ? errorCode.getHttpStatus().value() : 400;
    }

    public BookingException(BookingErrorCode errorCode, int statusCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
