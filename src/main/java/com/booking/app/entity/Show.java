package com.booking.app.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "price_paise", nullable = false)
    private long pricePaise;

    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit;

    @OneToMany(
            mappedBy = "show",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Seat> seats = new ArrayList<>();

    protected Show() {
    }

    public Show(String name, long pricePaise, int perUserLimit) {
        this.name = name;
        this.pricePaise = pricePaise;
        this.perUserLimit = perUserLimit;
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setShow(this);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getPricePaise() {
        return pricePaise;
    }

    public int getPerUserLimit() {
        return perUserLimit;
    }

    public List<Seat> getSeats() {
        return seats;
    }
}
