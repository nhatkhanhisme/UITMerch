package com.uitmerch.backend.common.model;

/**
 * User roles as per SRS requirements.
 * BR01 requires email globally unique.
 * BR02 requires password >= 8 chars with uppercase, lowercase, number.
 */
public enum UserRole {
    CUSTOMER,      // End user purchasing merch
    ORGANIZER,     // Club/organization admin managing merch catalog
    ADMIN          // Platform admin for governance
}
