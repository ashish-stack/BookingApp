package com.booking.app.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reservation_idempotency",
                        columnNames = {
                                "show_id",
                                "user_id",
                                "idempotency_key"
                        }
                )
        }
)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "user_id", nullable = false, length = 200)
    private String userId;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Reservation() {
    }

    public Reservation(
            Show show,
            String userId,
            String idempotencyKey,
            String requestHash,
            long amountPaise
    ) {
        this.show = show;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.amountPaise = amountPaise;
        this.status = ReservationStatus.CONFIRMED;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Show getShow() {
        return show;
    }

    public String getUserId() {
        return userId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }
}
