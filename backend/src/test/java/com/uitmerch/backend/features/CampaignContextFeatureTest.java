package com.uitmerch.backend.features;

import com.uitmerch.backend.campaign.*;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.order.dto.InstantOrderRequest;
import com.uitmerch.backend.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
class CampaignContextFeatureTest extends BackendFeatureTest {
    @Autowired CampaignReadService reads;
    @Autowired CampaignService campaigns;
    @Autowired OrderService orders;
    @Autowired JdbcTemplate jdbc;

    @Test void publishedPurchaseContextMatchesOrdinaryCheckoutPolicyIncludingOverdueActiveCampaign() {
        var org = organization(); var item = product(org, 10);
        assertThat(reads.purchaseContext(item.getId()).reservationRequired()).isFalse();
        var campaign = campaigns.create(org.getOwnerId(), org.getId(), new CampaignRequests.Create("Context", null, 2,
                Instant.now().plusSeconds(3600), List.of(new CampaignRequests.Variant(item.getId(), "Blue"))));
        assertThat(reads.purchaseContext(item.getId()).campaignId()).isEqualTo(campaign.id());
        jdbc.update("UPDATE preorder_campaigns SET deadline=? WHERE id=?", java.sql.Timestamp.from(Instant.now().minusSeconds(5)), campaign.id());
        assertThat(reads.purchaseContext(item.getId()).reservationRequired()).isTrue();
        campaigns.finalizeDue(campaign.id());
        assertThat(reads.purchaseContext(item.getId()).reservationRequired()).isFalse();
        item.setStatus(MerchItemStatus.DRAFT); merch.saveAndFlush(item);
        assertThatThrownBy(() -> reads.purchaseContext(item.getId())).isInstanceOf(ResourceNotFoundException.class);
        item.setStatus(MerchItemStatus.PUBLISHED); merch.saveAndFlush(item);
        org.setStatus(OrganizationStatus.INACTIVE); organizations.saveAndFlush(org);
        assertThatThrownBy(() -> reads.purchaseContext(item.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void contextUsesOrderReservationRatherThanCurrentSkuCampaignAndTracksClosedState() {
        var org = organization(); var item = product(org, 10); var customer = user(UserRole.CUSTOMER);
        var request = new InstantOrderRequest(); request.setMerchId(item.getId()); request.setQuantity(1);
        var normal = orders.createInstantOrder(customer.getId(), request);
        var campaign = campaigns.create(org.getOwnerId(), org.getId(), new CampaignRequests.Create("Context", null, 2,
                Instant.now().plusSeconds(3600), List.of(new CampaignRequests.Variant(item.getId(), "Blue"))));
        var reserved = campaigns.reserve(customer.getId(), campaign.id(), new CampaignRequests.Reserve(item.getId(), 2, UUID.randomUUID(), null));
        assertThat(reads.customerOrder(customer.getId(), normal.getId()).fulfillmentAllowed()).isTrue();
        assertThat(reads.customerOrder(customer.getId(), normal.getId()).campaignId()).isNull();
        assertThat(reads.organizationOrder(org.getOwnerId(), org.getId(), reserved.order().getId()).fulfillmentAllowed()).isFalse();
        jdbc.update("UPDATE preorder_campaigns SET deadline=? WHERE id=?", java.sql.Timestamp.from(Instant.now().minusSeconds(5)), campaign.id());
        campaigns.finalizeDue(campaign.id());
        assertThat(reads.customerOrder(customer.getId(), reserved.order().getId()).campaignState()).isEqualTo(Campaign.State.SUCCEEDED);
        assertThat(reads.customerOrder(customer.getId(), reserved.order().getId()).fulfillmentAllowed()).isTrue();
    }

    @Test void ownDetailRemainsReadableForInactiveOrgAndCancelledReservationQuantityIsExcluded() {
        var org = organization(); var item = product(org, 10); var customer = user(UserRole.CUSTOMER);
        var campaign = campaigns.create(org.getOwnerId(), org.getId(), new CampaignRequests.Create("Context", null, 5,
                Instant.now().plusSeconds(3600), List.of(new CampaignRequests.Variant(item.getId(), "Blue"))));
        var reserved = campaigns.reserve(customer.getId(), campaign.id(), new CampaignRequests.Reserve(item.getId(), 2, UUID.randomUUID(), null));
        assertThat(reads.organizationDetail(org.getOwnerId(), org.getId(), campaign.id()).reservedQuantity()).isEqualTo(2);
        orders.cancelCustomerOrder(customer.getId(), reserved.order().getId(), new com.uitmerch.backend.order.dto.CancelOrderRequest());
        assertThat(reads.organizationDetail(org.getOwnerId(), org.getId(), campaign.id()).reservedQuantity()).isZero();
        org.setStatus(OrganizationStatus.INACTIVE); organizations.saveAndFlush(org);
        assertThat(reads.organizationDetail(org.getOwnerId(), org.getId(), campaign.id()).variants().getFirst().available()).isFalse();
        assertThatThrownBy(() -> reads.organizationDetail(UUID.randomUUID(), org.getId(), campaign.id())).isInstanceOf(ResourceNotFoundException.class);
        campaigns.cancel(org.getOwnerId(), org.getId(), campaign.id());
        assertThat(reads.customerOrder(customer.getId(), reserved.order().getId()).campaignState()).isEqualTo(Campaign.State.CANCELLED);
        assertThat(reads.customerOrder(customer.getId(), reserved.order().getId()).fulfillmentAllowed()).isFalse();
    }

    @Test void apiEnforcesRolesAndOwnershipWithoutLeakingContext() throws Exception {
        var org = organization(); var otherOrg = organization(); var item = product(org, 10); var customer = user(UserRole.CUSTOMER);
        var request = new InstantOrderRequest(); request.setMerchId(item.getId()); request.setQuantity(1);
        var order = orders.createInstantOrder(customer.getId(), request);
        String customerPath = "/api/v1/customer/orders/" + order.getId() + "/campaign-context";
        String orgPath = "/api/v1/organizations/" + org.getId() + "/orders/" + order.getId() + "/campaign-context";
        mvc.perform(get(customerPath)).andExpect(status().isUnauthorized());
        mvc.perform(get(customerPath).header("Authorization", "Bearer " + token(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.data.fulfillmentAllowed").value(true));
        mvc.perform(get(customerPath).header("Authorization", "Bearer " + token(user(UserRole.CUSTOMER)))).andExpect(status().isNotFound());
        mvc.perform(get(orgPath).header("Authorization", "Bearer " + token(customer))).andExpect(status().isForbidden());
        mvc.perform(get(orgPath).header("Authorization", "Bearer " + token(users.findById(otherOrg.getOwnerId()).orElseThrow()))).andExpect(status().isNotFound());
        mvc.perform(get(orgPath).header("Authorization", "Bearer " + token(users.findById(org.getOwnerId()).orElseThrow()))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/organizations/" + otherOrg.getId() + "/orders/" + order.getId() + "/campaign-context")
                .header("Authorization", "Bearer " + token(users.findById(otherOrg.getOwnerId()).orElseThrow()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/public/merch/" + UUID.randomUUID() + "/purchase-context")).andExpect(status().isNotFound());
    }
}
