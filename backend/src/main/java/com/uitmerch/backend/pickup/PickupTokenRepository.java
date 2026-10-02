package com.uitmerch.backend.pickup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PickupTokenRepository extends JpaRepository<PickupToken, UUID> {
    Optional<PickupToken> findByTokenHash(String hash);
    @org.springframework.data.jpa.repository.Query("SELECT t.orderId FROM PickupToken t WHERE t.tokenHash = :hash")
    Optional<UUID> findOrderIdByTokenHash(@org.springframework.data.repository.query.Param("hash") String hash);
}
