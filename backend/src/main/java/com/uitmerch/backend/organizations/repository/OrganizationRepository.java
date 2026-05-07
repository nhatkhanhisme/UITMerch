package com.uitmerch.backend.organizations.repository;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.organizations.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Organization entity.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findTopByOwnerUserOrderByCreatedAtAsc(User ownerUser);

    Optional<Organization> findByIdAndOwnerUser(UUID id, User ownerUser);
}