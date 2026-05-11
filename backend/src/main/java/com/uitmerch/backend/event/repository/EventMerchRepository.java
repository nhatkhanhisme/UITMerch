package com.uitmerch.backend.event.repository;

import com.uitmerch.backend.event.entity.EventMerch;
import com.uitmerch.backend.event.entity.EventMerchId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventMerchRepository extends JpaRepository<EventMerch, EventMerchId> {

    List<EventMerch> findByEventId(UUID eventId);

    boolean existsByEventIdAndMerchId(UUID eventId, UUID merchId);

    @Transactional
    void deleteByEventIdAndMerchId(UUID eventId, UUID merchId);
}
