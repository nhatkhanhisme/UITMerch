package com.uitmerch.backend.campaign;
import com.uitmerch.backend.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;
/** Called after locking merchandise. Never lock a campaign from a held order or stock lock. */
@Service @RequiredArgsConstructor
public class CampaignOrderPolicy {
    private final CampaignVariantRepository variants;
    private final CampaignReservationRepository reservations;
    private final CampaignRepository campaigns;
    public void validateMerch(UUID merch,UUID campaign) {
        var active=variants.activeCampaigns(merch);
        if(campaign==null && !active.isEmpty()) throw new ValidationException("Reserve this item through its preorder campaign.");
        if(campaign!=null && (active.size()!=1 || !active.contains(campaign))) throw new ValidationException("Campaign is not accepting reservations.");
    }
    public BigDecimal price(UUID campaign,UUID merch) {
        return variants.findByCampaignIdAndMerchId(campaign,merch).orElseThrow(()->new ValidationException("Invalid campaign variant.")).getUnitPrice();
    }
    public void ensureCanProgress(UUID orderId) {
        reservations.findByOrderId(orderId).ifPresent(r->{
            if(campaigns.findById(r.getCampaignId()).orElseThrow().getState()!=Campaign.State.SUCCEEDED)
                throw new ValidationException("Preorder campaign must succeed before this order can progress.");
        });
    }
}
