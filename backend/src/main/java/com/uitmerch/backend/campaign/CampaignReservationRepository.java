package com.uitmerch.backend.campaign;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface CampaignReservationRepository extends JpaRepository<CampaignReservation,UUID> {
    Optional<CampaignReservation> findByUserIdAndRequestId(UUID userId,UUID requestId);
    Optional<CampaignReservation> findByOrderId(UUID orderId);
    Page<CampaignReservation> findByUserId(UUID userId,Pageable page);
    @Query("select r.orderId from CampaignReservation r where r.campaignId=:campaign order by r.orderId")
    List<UUID> orderIds(@Param("campaign") UUID campaignId);
}
