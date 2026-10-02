package com.uitmerch.backend.campaign;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record CampaignResponse(UUID id,UUID orgId,String title,String description,int minimumQuantity,
        Instant deadline,Campaign.State state,long reservedQuantity,Instant createdAt,List<Variant> variants) {
    public record Variant(UUID merchId,String label,BigDecimal unitPrice,int availableQuantity,boolean available) {}
    static CampaignResponse from(Campaign c,long quantity,List<Variant> variants) {
        return new CampaignResponse(c.getId(),c.getOrgId(),c.getTitle(),c.getDescription(),c.getMinimumQuantity(),
            c.getDeadline(),c.getState(),quantity,c.getCreatedAt(),variants);
    }
}
