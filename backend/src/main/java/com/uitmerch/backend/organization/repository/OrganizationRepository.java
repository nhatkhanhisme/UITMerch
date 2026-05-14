package com.uitmerch.backend.organization.repository;

import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.organization.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Page<Organization> findByOwnerId(UUID ownerId, Pageable pageable);

    Optional<Organization> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);
}
