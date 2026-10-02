package com.uitmerch.backend.pickup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface GuestPickupReceiptRepository extends JpaRepository<GuestPickupReceipt, UUID> {}
