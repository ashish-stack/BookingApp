package com.booking.app.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BookingException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}