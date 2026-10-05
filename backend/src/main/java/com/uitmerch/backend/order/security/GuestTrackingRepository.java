package com.uitmerch.backend.order.security;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface GuestTrackingRepository extends JpaRepository<GuestTrackingCredential,UUID> {}
