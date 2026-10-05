package com.uitmerch.backend.order.security;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CheckoutAttemptRepository extends JpaRepository<CheckoutAttempt,String> {}
