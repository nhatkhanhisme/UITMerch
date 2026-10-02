package com.uitmerch.backend.event.repository;

import com.uitmerch.backend.common.model.EventStatus;
import com.uitmerch.backend.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    Page<Event> findByOrgId(UUID orgId, Pageable pageable);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Event e WHERE e.status IN :statuses AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = e.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<Event> findByStatusIn(Collection<EventStatus> statuses, Pageable pageable);

    Page<Event> findByOrgIdAndStatus(UUID orgId, EventStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Event e WHERE e.orgId = :orgId AND e.status IN :statuses AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = e.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<Event> findByOrgIdAndStatusIn(UUID orgId, Collection<EventStatus> statuses, Pageable pageable);

    Optional<Event> findByIdAndOrgId(UUID id, UUID orgId);
}
