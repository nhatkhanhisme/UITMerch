package com.uitmerch.backend.restock;

import java.time.Instant;
import java.util.UUID;

public record RestockDetails(UUID merchId, String merchName, String orgName, boolean available,
    boolean emailEnabled, Instant subscribedAt) {}
