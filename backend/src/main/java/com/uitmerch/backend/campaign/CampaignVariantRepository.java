package com.uitmerch.backend.campaign;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CampaignVariantRepository extends JpaRepository<CampaignVariant,UUID> {
    List<CampaignVariant> findByCampaignIdOrderById(UUID campaignId);
    Optional<CampaignVariant> findByCampaignIdAndMerchId(UUID campaignId,UUID merchId);
    @Query("select c.id from Campaign c, CampaignVariant v where v.campaignId=c.id and v.merchId=:merch and c.state=com.uitmerch.backend.campaign.Campaign$State.ACTIVE")
    List<UUID> activeCampaigns(@Param("merch") UUID merchId);
}
