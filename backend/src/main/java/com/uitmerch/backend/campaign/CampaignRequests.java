package com.uitmerch.backend.campaign;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public final class CampaignRequests {
    private CampaignRequests() {}
    public record Variant(@NotNull UUID merchId,@NotBlank @Size(max=128) String label) {}
    public record Create(@NotBlank @Size(max=255) String title,@Size(max=4000) String description,
            @Min(1) int minimumQuantity,@NotNull @Future Instant deadline,
            @NotEmpty @Size(max=20) List<@NotNull @Valid Variant> variants) {}
    public record Reserve(@NotNull UUID merchId,@Min(1) @Max(100) int quantity,
            @NotNull UUID requestId,@Size(max=1000) String note) {}
}
