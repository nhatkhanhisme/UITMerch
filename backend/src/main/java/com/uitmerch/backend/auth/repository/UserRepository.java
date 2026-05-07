package com.uitmerch.backend.auth.repository;

import com.uitmerch.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find user by email.
     * BR01: Email is globally unique.
     * @param email user email
     * @return optional containing user if found
     */
    Optional<User> findByEmail(String email);
}
