package com.booking.app.service;

import com.booking.app.exception.BookingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public String requireUser(String authorization) {

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            throw new BookingException(
                    HttpStatus.UNAUTHORIZED,
                    "missing_or_invalid_authorization"
            );
        }

        String token = authorization.substring(7).trim();

        if (token.isBlank()) {
            throw new BookingException(
                    HttpStatus.UNAUTHORIZED,
                    "missing_or_invalid_authorization"
            );
        }

        return token;
    }

    public void requireAdmin(String authorization) {

        String user = requireUser(authorization);

        if (!"admin".equals(user)) {
            throw new BookingException(
                    HttpStatus.FORBIDDEN,
                    "admin_required"
            );
        }
    }
}
