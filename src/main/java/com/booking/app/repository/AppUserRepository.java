package com.booking.app.repository;

import com.booking.app.entity.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    @Modifying
    @Query(
            value = """
                    MERGE INTO app_users (user_id)
                    KEY (user_id)
                    VALUES (:userId)
                    """,
            nativeQuery = true
    )
    void ensureExists(@Param("userId") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<AppUser> findLockedByUserId(String userId);
}