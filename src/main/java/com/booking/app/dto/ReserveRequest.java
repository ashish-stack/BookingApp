package com.booking.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReserveRequest(

        @NotEmpty
        List<String> seats,

        @JsonProperty("idempotency_key")
        String idempotencyKey
) {
}