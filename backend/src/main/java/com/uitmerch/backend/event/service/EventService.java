package com.uitmerch.backend.event.service;

import com.uitmerch.backend.common.exception.ConflictException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.EventStatus;
import com.uitmerch.backend.event.dto.AttachMerchRequest;
import com.uitmerch.backend.event.dto.CreateEventRequest;
import com.uitmerch.backend.event.dto.EventResponse;
import com.uitmerch.backend.event.dto.UpdateEventRequest;
import com.uitmerch.backend.event.entity.Event;
import com.uitmerch.backend.event.entity.EventMerch;
import com.uitmerch.backend.event.repository.EventMerchRepository;
import com.uitmerch.backend.event.repository.EventRepository;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMerchRepository eventMerchRepository;
    private final MerchItemRepository merchItemRepository;
    private final OrganizationService organizationService;

    @Transactional
    public EventResponse createEvent(UUID ownerId, CreateEventRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);

        Event event = Event.builder()
            .orgId(org.getId())
            .title(request.getTitle())
            .description(request.getDescription())
            .coverUrl(request.getCoverUrl())
            .startsAt(request.getStartsAt())
            .endsAt(request.getEndsAt())
            .build();

        return EventResponse.from(eventRepository.save(event));
    }

    public Page<EventResponse> getOwnEvents(UUID ownerId, Pageable pageable) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        return eventRepository.findByOrgId(org.getId(), pageable)
            .map(EventResponse::from);
    }

    public EventResponse getOwnEvent(UUID ownerId, UUID eventId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        Event event = eventRepository.findByIdAndOrgId(eventId, org.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Event", eventId.toString()));
        List<MerchResponse> merch = fetchMerchForEvent(eventId);
        return EventResponse.from(event, merch);
    }

    @Transactional
    public EventResponse updateEvent(UUID ownerId, UUID eventId, UpdateEventRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        Event event = eventRepository.findByIdAndOrgId(eventId, org.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Event", eventId.toString()));

        if (request.getStatus() != null && !request.getStatus().equals(event.getStatus())) {
            validateStatusTransition(event.getStatus(), request.getStatus());
            event.setStatus(request.getStatus());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            event.setCoverUrl(request.getCoverUrl());
        }
        if (request.getStartsAt() != null) {
            event.setStartsAt(request.getStartsAt());
        }
        if (request.getEndsAt() != null) {
            event.setEndsAt(request.getEndsAt());
        }

        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse attachMerch(UUID ownerId, UUID eventId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        Event event = eventRepository.findByIdAndOrgId(eventId, org.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Event", eventId.toString()));

        // BR13: merch must belong to the same org
        boolean merchBelongsToOrg = merchItemRepository.existsByIdAndOrgId(merchId, org.getId());
        if (!merchBelongsToOrg) {
            throw new ValidationException("Merch item does not belong to your organization.");
        }

        if (eventMerchRepository.existsByEventIdAndMerchId(eventId, merchId)) {
            throw new ConflictException("Merch item is already attached to this event.");
        }

        EventMerch eventMerch = EventMerch.builder()
            .eventId(eventId)
            .merchId(merchId)
            .build();
        eventMerchRepository.save(eventMerch);

        List<MerchResponse> merch = fetchMerchForEvent(eventId);
        return EventResponse.from(event, merch);
    }

    @Transactional
    public void detachMerch(UUID ownerId, UUID eventId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        eventRepository.findByIdAndOrgId(eventId, org.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Event", eventId.toString()));

        if (!eventMerchRepository.existsByEventIdAndMerchId(eventId, merchId)) {
            throw new ResourceNotFoundException("Merch item is not attached to this event.");
        }

        eventMerchRepository.deleteByEventIdAndMerchId(eventId, merchId);
    }

    private static final Set<EventStatus> PUBLIC_STATUSES = Set.of(EventStatus.PUBLISHED, EventStatus.ENDED);

    public Page<EventResponse> getPublicEvents(Pageable pageable) {
        return eventRepository.findByStatusIn(PUBLIC_STATUSES, pageable)
            .map(EventResponse::from);
    }

    public Page<EventResponse> getPublicEventsByOrg(UUID orgId, Pageable pageable) {
        return eventRepository.findByOrgIdAndStatusIn(orgId, PUBLIC_STATUSES, pageable)
            .map(EventResponse::from);
    }

    public EventResponse getPublicEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event", eventId.toString()));

        if (!PUBLIC_STATUSES.contains(event.getStatus())) {
            throw new ResourceNotFoundException("Event", eventId.toString());
        }

        List<MerchResponse> merch = fetchMerchForEvent(eventId);
        return EventResponse.from(event, merch);
    }

    // ------------------------------------------------------------------ //
    //  HELPERS
    // ------------------------------------------------------------------ //

    private List<MerchResponse> fetchMerchForEvent(UUID eventId) {
        List<UUID> merchIds = eventMerchRepository.findByEventId(eventId)
            .stream()
            .map(em -> em.getMerchId())
            .toList();
        return merchItemRepository.findAllById(merchIds)
            .stream()
            .map(MerchResponse::from)
            .toList();
    }

    private void validateStatusTransition(EventStatus current, EventStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == EventStatus.PUBLISHED;
            case PUBLISHED -> next == EventStatus.ENDED;
            case ENDED -> false;
        };

        if (!valid) {
            throw new ValidationException("Invalid event status transition.");
        }
    }
}
