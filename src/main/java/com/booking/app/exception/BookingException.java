package com.booking.app.exception;

import org.springframework.http.HttpStatus;

public class BookingException extends RuntimeException {

    private final HttpStatus status;
    private final String reason;

    public BookingException(HttpStatus status, String reason) {
        super(reason);
        this.status = status;
        this.reason = reason;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}