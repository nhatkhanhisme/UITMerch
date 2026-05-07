package com.uitmerch.backend.collections.service;

import java.util.UUID;

/**
 * Collection write service.
 */
public interface CollectionService {

    void createCollectionsForSuccessfulOrder(UUID orderId);
}