package com.uitmerch.backend.common.service;
import com.uitmerch.backend.common.security.SecurityCredentials;
import com.uitmerch.backend.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class SharedRateLimitStore {
    private final JdbcTemplate jdbc;
    /** A durable fixed window per key, serialized across application instances. */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public boolean allowed(String key,int maximum,Duration window) {
        if (maximum<1 || window.isZero() || window.isNegative()) throw new IllegalArgumentException("Invalid rate limit");
        String hash=SecurityCredentials.hash(key);
        try {
            String dialect=jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>) c->c.getMetaData().getDatabaseProductName());
            if ("PostgreSQL".equals(dialect))
                jdbc.update("INSERT INTO rate_limit_buckets(key_hash,window_start,attempts,expires_at) VALUES (?,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",hash);
            else jdbc.update("MERGE INTO rate_limit_buckets t USING (VALUES (?)) s(key_hash) ON t.key_hash=s.key_hash WHEN NOT MATCHED THEN INSERT(key_hash,window_start,attempts,expires_at) VALUES(s.key_hash,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP)",hash);
            var row=jdbc.queryForMap("SELECT attempts,expires_at FROM rate_limit_buckets WHERE key_hash=? FOR UPDATE",hash);
            Instant now=jdbc.queryForObject("SELECT CURRENT_TIMESTAMP",java.sql.Timestamp.class).toInstant();
            Object storedExpiry=row.get("expires_at");
            Instant expiry=storedExpiry instanceof OffsetDateTime date?date.toInstant():((java.sql.Timestamp)storedExpiry).toInstant();
            int count=((Number)row.get("attempts")).intValue();
            if (!expiry.isAfter(now)) { count=0;expiry=now.plus(window); }
            if (count>=maximum) return false;
            jdbc.update("UPDATE rate_limit_buckets SET attempts=?,expires_at=? WHERE key_hash=?",count+1,java.sql.Timestamp.from(expiry),hash);
            return true;
        } catch (org.springframework.dao.DataAccessException ex) {
            throw new AppException("Request protection temporarily unavailable. Try again later.",HttpStatus.SERVICE_UNAVAILABLE,"RATE_LIMIT_UNAVAILABLE");
        }
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void purge() { jdbc.update("DELETE FROM rate_limit_buckets WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1' HOUR"); }
}
