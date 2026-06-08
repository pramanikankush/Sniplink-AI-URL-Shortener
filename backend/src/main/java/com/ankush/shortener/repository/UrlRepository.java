package com.ankush.shortener.repository;

import com.ankush.shortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByCode(String code);

    Optional<Url> findByLongUrl(String longUrl);

    boolean existsByCode(String code);

    /**
     * Atomic increment — avoids a read-modify-write race without a cache layer.
     * Returns the number of rows updated so callers can detect a vanished row.
     */
    @Modifying
    @Query("UPDATE Url u SET u.clicks = u.clicks + 1 WHERE u.code = :code")
    int incrementClicks(@Param("code") String code);
}
