package com.roomsync.booking.exception;

import com.roomsync.booking.dto.response.ApiResponse;
import com.roomsync.booking.enums.BookingErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingException(BookingException ex, HttpServletRequest request) {
        BookingErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = errorCode != null ? errorCode.getHttpStatus() : HttpStatus.valueOf(ex.getStatusCode());
        String codeName = errorCode != null ? errorCode.name() : "BOOKING_ERROR";

        log.warn("Nghiệp vụ từ chối [{} - {}]: {}", status.value(), codeName, ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(status.value(), codeName, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Tham số đầu vào không hợp lệ: {}", details);

        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), BookingErrorCode.INVALID_REQUEST.name(), details, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Đối số không hợp lệ: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), BookingErrorCode.INVALID_REQUEST.name(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Lỗi hệ thống không xử lý: ", ex);
        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                BookingErrorCode.INTERNAL_SERVER_ERROR.name(),
                ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi hệ thống nội bộ!",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
