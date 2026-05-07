package com.uitmerch.backend.common.model;

/**
 * Organization approval status as per SRS.
 */
public enum OrganizationStatus {
    PENDING,      // Awaiting admin approval (FR10)
    ACTIVE,       // Approved and can sell merch
    INACTIVE      // Deactivated org
}
