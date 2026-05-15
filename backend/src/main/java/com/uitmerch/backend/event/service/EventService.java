package com.uitmerch.backend.event.service;

import com.uitmerch.backend.common.exception.ConflictException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.EventStatus;
import com.uitmerch.backend.event.dto.CreateEventRequest;
import com.uitmerch.backend.event.dto.EventResponse;
import com.uitmerch.backend.event.dto.UpdateEventRequest;
import com.uitmerch.backend.event.entity.Event;
import com.uitmerch.backend.event.entity.EventMerch;
import com.uitmerch.backend.event.repository.EventMerchRepository;
import com.uitmerch.backend.event.repository.EventRepository;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.entity.Category;
import com.uitmerch.backend.merch.entity.MerchImage;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.CategoryRepository;
import com.uitmerch.backend.merch.repository.MerchImageRepository;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMerchRepository eventMerchRepository;
    private final MerchItemRepository merchItemRepository;
    private final MerchImageRepository merchImageRepository;
    private final CategoryRepository categoryRepository;
    private final OrganizationService organizationService;

    @Transactional
    public EventResponse createEvent(UUID ownerId, UUID orgId, CreateEventRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);

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

    @Transactional(readOnly = true)
    public Page<EventResponse> getOwnEvents(UUID ownerId, UUID orgId, Pageable pageable) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
        return eventRepository.findByOrgId(org.getId(), pageable)
            .map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse getOwnEvent(UUID ownerId, UUID orgId, UUID eventId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
        Event event = eventRepository.findByIdAndOrgId(eventId, org.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Event", eventId.toString()));
        List<MerchResponse> merch = fetchMerchForEvent(eventId);
        return EventResponse.from(event, merch);
    }

    @Transactional
    public EventResponse updateEvent(UUID ownerId, UUID orgId, UUID eventId, UpdateEventRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
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
    public EventResponse attachMerch(UUID ownerId, UUID orgId, UUID eventId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
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
    public void detachMerch(UUID ownerId, UUID orgId, UUID eventId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
        eventRepository.findByIdAndOrgId(eventId, org.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Event", eventId.toString()));

        if (!eventMerchRepository.existsByEventIdAndMerchId(eventId, merchId)) {
            throw new ResourceNotFoundException("Merch item is not attached to this event.");
        }

        eventMerchRepository.deleteByEventIdAndMerchId(eventId, merchId);
    }

    private static final Set<EventStatus> PUBLIC_STATUSES = Set.of(EventStatus.PUBLISHED, EventStatus.ENDED);

    @Transactional(readOnly = true)
    public Page<EventResponse> getPublicEvents(Pageable pageable) {
        return eventRepository.findByStatusIn(PUBLIC_STATUSES, pageable)
            .map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getPublicEventsByOrg(UUID orgId, Pageable pageable) {
        return eventRepository.findByOrgIdAndStatusIn(orgId, PUBLIC_STATUSES, pageable)
            .map(EventResponse::from);
    }

    @Transactional(readOnly = true)
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
            .map(EventMerch::getMerchId)
            .toList();

        List<MerchItem> items = merchItemRepository.findAllById(merchIds);

        List<UUID> categoryIds = items.stream()
            .map(MerchItem::getCategoryId)
            .filter(id -> id != null)
            .distinct()
            .toList();

        Map<UUID, Category> categoryMap = categoryRepository.findAllById(categoryIds)
            .stream()
            .collect(Collectors.toMap(Category::getId, c -> c));

        Map<UUID, List<String>> imageMap = merchImageRepository.findByMerchIdInOrderByPosition(merchIds)
            .stream()
            .collect(Collectors.groupingBy(
                MerchImage::getMerchId,
                Collectors.mapping(MerchImage::getUrl, Collectors.toList())
            ));

        return items.stream()
            .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId()), imageMap.get(item.getId())))
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
