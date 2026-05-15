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
import com.uitmerch.backend.merch.repository.CategoryRepository;
import com.uitmerch.backend.merch.repository.MerchImageRepository;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventMerchRepository eventMerchRepository;
    @Mock private MerchItemRepository merchItemRepository;
    @Mock private MerchImageRepository merchImageRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OrganizationService organizationService;

    @InjectMocks private EventService eventService;

    private final UUID ownerId  = UUID.randomUUID();
    private final UUID orgId    = UUID.randomUUID();
    private final UUID eventId  = UUID.randomUUID();
    private final UUID merchId  = UUID.randomUUID();

    private Organization org() {
        return Organization.builder().id(orgId).ownerId(ownerId).build();
    }

    private Event event(EventStatus status) {
        return Event.builder().id(eventId).orgId(orgId).title("T").status(status).build();
    }

    // ── createEvent ──────────────────────────────────────────────────────────

    @Test
    void createEvent_success_savesAndReturns() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(org());
        when(eventRepository.save(any())).thenReturn(event(EventStatus.DRAFT));

        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("UIT Hackathon 2025");

        EventResponse response = eventService.createEvent(ownerId, orgId, req);

        assertThat(response).isNotNull();
        verify(eventRepository).save(any());
    }

    // ── updateEvent: status transitions ─────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"DRAFT,PUBLISHED", "PUBLISHED,ENDED"})
    void updateEvent_validStatusTransition_succeeds(EventStatus from, EventStatus to) {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(org());
        when(eventRepository.findByIdAndOrgId(eventId, orgId)).thenReturn(Optional.of(event(from)));
        when(eventRepository.save(any())).thenReturn(event(to));

        UpdateEventRequest req = new UpdateEventRequest();
        req.setStatus(to);

        assertThat(eventService.updateEvent(ownerId, orgId, eventId, req)).isNotNull();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"DRAFT,ENDED", "PUBLISHED,DRAFT", "ENDED,DRAFT", "ENDED,PUBLISHED"})
    void updateEvent_invalidStatusTransition_throwsValidation(EventStatus from, EventStatus to) {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(org());
        when(eventRepository.findByIdAndOrgId(eventId, orgId)).thenReturn(Optional.of(event(from)));

        UpdateEventRequest req = new UpdateEventRequest();
        req.setStatus(to);

        assertThatThrownBy(() -> eventService.updateEvent(ownerId, orgId, eventId, req))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateEvent_eventNotOwned_throwsResourceNotFound() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(org());
        when(eventRepository.findByIdAndOrgId(eventId, orgId)).thenReturn(Optional.empty());

        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("New Title");

        assertThatThrownBy(() -> eventService.updateEvent(ownerId, orgId, eventId, req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── attachMerch ──────────────────────────────────────────────────────────

    @Test
    void attachMerch_success_savesLink() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(org());
        when(eventRepository.findByIdAndOrgId(eventId, orgId)).thenReturn(Optional.of(event(EventStatus.DRAFT)));
        when(merchItemRepository.existsByIdAndOrgId(merchId, orgId)).thenReturn(true);
        when(eventMerchRepository.existsByEventIdAndMerchId(eventId, merchId)).thenReturn(false);
        when(eventMerchRepository.save(any())).thenReturn(EventMerch.builder().eventId(eventId).merchId(merchId).build());
        when(eventMerchRepository.findByEventId(eventId)).thenReturn(List.of());
        when(merchItemRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(merchImageRepository.findByMerchIdInOrderByPosition(any())).thenReturn(Collections.emptyList());

        eventService.attachMerch(ownerId, orgId, eventId, merchId);

        verify(eventMerchRepository).save(any());
    }

    @Test
    void attachMerch_merchBelongsToDifferentOrg_throwsValidation() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(org());
        when(eventRepository.findByIdAndOrgId(eventId, orgId)).thenReturn(Optional.of(event(EventStatus.DRAFT)));
        when(merchItemRepository.existsByIdAndOrgId(merchId, orgId)).thenReturn(false);

        assertThatThrownBy(() -> eventService.attachMerch(ownerId, orgId, eventId, merchId))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("belong to your organization");
        verify(eventMerchRepository, never()).save(any());
    }

    @Test
    void attachMerch_alreadyAttached_throwsConflict() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(org());
        when(eventRepository.findByIdAndOrgId(eventId, orgId)).thenReturn(Optional.of(event(EventStatus.DRAFT)));
        when(merchItemRepository.existsByIdAndOrgId(merchId, orgId)).thenReturn(true);
        when(eventMerchRepository.existsByEventIdAndMerchId(eventId, merchId)).thenReturn(true);

        assertThatThrownBy(() -> eventService.attachMerch(ownerId, orgId, eventId, merchId))
            .isInstanceOf(ConflictException.class);
    }

    // ── getPublicEvent ───────────────────────────────────────────────────────

    @Test
    void getPublicEvent_publishedEvent_returnsResponse() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(EventStatus.PUBLISHED)));
        when(eventMerchRepository.findByEventId(eventId)).thenReturn(Collections.emptyList());
        when(merchItemRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(merchImageRepository.findByMerchIdInOrderByPosition(any())).thenReturn(Collections.emptyList());

        EventResponse response = eventService.getPublicEvent(eventId);

        assertThat(response.getId()).isEqualTo(eventId);
    }

    @Test
    void getPublicEvent_draftEvent_throwsResourceNotFound() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event(EventStatus.DRAFT)));

        assertThatThrownBy(() -> eventService.getPublicEvent(eventId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublicEvent_notFound_throwsResourceNotFound() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getPublicEvent(eventId))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
