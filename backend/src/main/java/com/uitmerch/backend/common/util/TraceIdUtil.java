package com.uitmerch.backend.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility for trace ID generation and management.
 * Each request gets a unique trace ID for distributed tracing.
 */
public class TraceIdUtil {
    
    private static final String TRACE_ID_KEY = "traceId";
    
    /**
     * Get or create a trace ID for the current thread/request.
     * @return trace ID (UUID format)
     */
    public static String getOrCreateTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put(TRACE_ID_KEY, traceId);
        }
        return traceId;
    }
    
    /**
     * Set trace ID explicitly (useful for propagating from request headers).
     * @param traceId the trace ID to set
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }
    
    /**
     * Get current trace ID or null if not set.
     * @return trace ID or null
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
    
    /**
     * Clear trace ID (call on request cleanup).
     */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}
