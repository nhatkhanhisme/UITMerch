package com.uitmerch.backend.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uitmerch.backend.common.util.TraceIdUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope for all requests")
public class ApiResponse<T> {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private T data;

    @JsonProperty("meta")
    private PaginationMeta meta;

    @JsonProperty("traceId")
    private String traceId;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .traceId(TraceIdUtil.getTraceId())
            .build();
    }

    public static <T> ApiResponse<T> success(String message, T data, PaginationMeta meta) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .meta(meta)
            .traceId(TraceIdUtil.getTraceId())
            .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .traceId(TraceIdUtil.getTraceId())
            .build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .data(data)
            .traceId(TraceIdUtil.getTraceId())
            .build();
    }
}
