package com.booking.app.controller;

import com.booking.app.dto.CreateShowRequest;
import com.booking.app.dto.ShowResponse;
import com.booking.app.service.AuthService;
import com.booking.app.service.ShowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;
    private final AuthService authService;

    public ShowController(
            ShowService showService,
            AuthService authService
    ) {
        this.showService = showService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ShowResponse> createShow(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @Valid @RequestBody CreateShowRequest request
    ) {

        authService.requireAdmin(authorization);

        return ResponseEntity
                .status(201)
                .body(showService.createShow(request));
    }

    @GetMapping("/{showId}")
    public ResponseEntity<ShowResponse> getShow(
            @PathVariable UUID showId
    ) {

        return ResponseEntity.ok(
                showService.getShow(showId)
        );
    }
}