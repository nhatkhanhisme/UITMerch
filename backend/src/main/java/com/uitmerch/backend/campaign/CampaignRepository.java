package com.uitmerch.backend.campaign;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
public interface CampaignRepository extends JpaRepository<Campaign,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Campaign c where c.id=:id")
    Optional<Campaign> lock(@Param("id") UUID id);
    Page<Campaign> findByOrgId(UUID orgId, Pageable page);
    @Query("select c from Campaign c where exists (select o.id from Organization o where o.id=c.orgId and o.status=:#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<Campaign> findPublic(Pageable page);
    @Query("select c.id from Campaign c where c.state=com.uitmerch.backend.campaign.Campaign$State.ACTIVE and c.deadline<=:now order by c.deadline,c.id")
    List<UUID> due(@Param("now") Instant now, Pageable page);
}
