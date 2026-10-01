package com.uitmerch.backend.auth.repository;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.common.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findLockedByEmail(@org.springframework.data.repository.query.Param("email") String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(UserRole role, Pageable pageable);
}
