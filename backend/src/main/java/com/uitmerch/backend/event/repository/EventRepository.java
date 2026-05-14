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

    Page<Event> findByOrgId(UUID orgId, Pageable pageable);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByStatusIn(Collection<EventStatus> statuses, Pageable pageable);

    Page<Event> findByOrgIdAndStatus(UUID orgId, EventStatus status, Pageable pageable);

    Page<Event> findByOrgIdAndStatusIn(UUID orgId, Collection<EventStatus> statuses, Pageable pageable);

    Optional<Event> findByIdAndOrgId(UUID id, UUID orgId);
}
