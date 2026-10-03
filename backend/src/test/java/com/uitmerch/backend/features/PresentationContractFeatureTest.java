package com.uitmerch.backend.features;

import com.uitmerch.backend.following.*;
import com.uitmerch.backend.restock.RestockService;
import com.uitmerch.backend.common.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
class PresentationContractFeatureTest extends BackendFeatureTest {
    @Autowired FollowService follows;
    @Autowired RestockService restock;
    @Test void followingNamesAreScopedAndFollowerCountsExcludeDisabledOrInactiveAccounts() throws Exception {
        var org = organization(); var a = user(UserRole.CUSTOMER); var b = user(UserRole.CUSTOMER); var stranger = user(UserRole.CUSTOMER);
        follows.follow(a.getId(), org.getId(), null); follows.follow(b.getId(), org.getId(), null);
        mvc.perform(get("/api/v1/customer/following").header("Authorization", "Bearer " + token(a)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].orgName").value(org.getName()))
            .andExpect(jsonPath("$.data.content[0].orgStatus").value("ACTIVE"));
        assertThat(follows.listDetails(stranger.getId(), PageRequest.of(0,20))).isEmpty();
        mvc.perform(get("/api/v1/public/organizations/" + org.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.data.followerCount").value(2));
        follows.unfollow(b.getId(), org.getId());
        mvc.perform(get("/api/v1/public/organizations/" + org.getId())).andExpect(jsonPath("$.data.followerCount").value(1));
        a.setActive(false); users.save(a);
        mvc.perform(get("/api/v1/public/organizations/" + org.getId())).andExpect(jsonPath("$.data.followerCount").value(0));
    }
    @Test void restockIncludesProductIdentityAndCorrectAvailability() throws Exception {
        var org = organization(); var a = user(UserRole.CUSTOMER); var product = product(org,0);
        restock.subscribe(a.getId(), product.getId(), false);
        mvc.perform(get("/api/v1/customer/restock-subscriptions").header("Authorization", "Bearer " + token(a)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].merchName").value(product.getName()))
            .andExpect(jsonPath("$.data.content[0].orgName").value(org.getName()))
            .andExpect(jsonPath("$.data.content[0].available").value(false));
        product.setStock(5); merch.save(product);
        assertThat(restock.listDetails(a.getId(),PageRequest.of(0,20)).getContent().getFirst().available()).isTrue();
        org.setStatus(OrganizationStatus.INACTIVE); organizations.save(org);
        assertThat(restock.listDetails(a.getId(),PageRequest.of(0,20)).getContent().getFirst().available()).isFalse();
    }
    @Test void freeMerchCanBeCreatedAndPaidMerchUpdatedToZeroWithoutConstraintFailure() throws Exception {
        var org = organization(); var owner = users.findById(org.getOwnerId()).orElseThrow(); var bearer = "Bearer " + token(owner);
        var created = mvc.perform(post("/api/v1/organizations/" + org.getId() + "/merchs").header("Authorization",bearer)
            .contentType("application/json").content("{\"name\":\"Free sticker\",\"price\":0,\"stock\":5}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.price").value(0)).andReturn();
        UUID id = UUID.fromString(json.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText());
        assertThat(merch.findById(id).orElseThrow().getPrice()).isEqualByComparingTo("0");
        var paid = product(org,5);
        mvc.perform(patch("/api/v1/organizations/" + org.getId() + "/merchs/" + paid.getId()).header("Authorization",bearer)
            .contentType("application/json").content("{\"price\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.price").value(0));
        assertThat(merch.findById(paid.getId()).orElseThrow().getPrice()).isEqualByComparingTo("0");
        mvc.perform(patch("/api/v1/organizations/" + org.getId() + "/merchs/" + paid.getId()).header("Authorization",bearer)
            .contentType("application/json").content("{\"price\":-1}"))
            .andExpect(status().isBadRequest());
    }
    @Test void adminContractSerializesActualAccountActivationAndVerification() throws Exception {
        var admin = user(UserRole.ADMIN); var a = user(UserRole.CUSTOMER); var bearer = "Bearer " + token(admin);
        mvc.perform(patch("/api/v1/admin/users/" + a.getId() + "/active").param("active","false").header("Authorization",bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false)).andExpect(jsonPath("$.data.verified").value(true));
        mvc.perform(patch("/api/v1/admin/users/" + a.getId() + "/active").param("active","true").header("Authorization",bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
    }
}
