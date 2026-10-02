package com.uitmerch.backend.campaign;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="campaign_variants",uniqueConstraints=@UniqueConstraint(columnNames={"campaign_id","merch_id"}))
@Getter @Setter @NoArgsConstructor
public class CampaignVariant {
    @Id private UUID id;
    @Column(name="campaign_id",nullable=false) private UUID campaignId;
    @Column(name="merch_id",nullable=false) private UUID merchId;
    @Column(nullable=false,length=128) private String label;
    @Column(name="unit_price",nullable=false,precision=12,scale=2) private BigDecimal unitPrice;
}
