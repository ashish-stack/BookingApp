package com.booking.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @Column(name = "user_id", nullable = false, length = 200)
    private String userId;

    protected AppUser() {
    }

    public AppUser(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}