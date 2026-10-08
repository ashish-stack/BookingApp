package com.booking.app.exception;

import com.booking.app.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ErrorResponse> handleBookingException(
            BookingException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        Instant.now(),
                        ex.getStatus().value(),
                        ex.getStatus().getReasonPhrase(),
                        ex.getReason(),
                        request.getHeader("X-Request-ID")
                );

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response =
                new ErrorResponse(
                        Instant.now(),
                        404,
                        "Not Found",
                        ex.getReason(),
                        request.getHeader("X-Request-ID")
                );

        return ResponseEntity
                .status(404)
                .body(response);
    }
}
