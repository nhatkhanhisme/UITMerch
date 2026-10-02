package com.uitmerch.backend.features;
import com.uitmerch.backend.common.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class ApiBoundaryFeatureTest extends BackendFeatureTest {
    @Test void invalidDatesAndIdentifiersReturnBadRequest() throws Exception {
        var org=organization(); var owner=users.findById(org.getOwnerId()).orElseThrow();
        mvc.perform(get("/api/v1/organizations/"+org.getId()+"/analytics").param("from","not-a-date")
            .header("Authorization","Bearer "+token(owner))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/public/campaigns/not-a-uuid")).andExpect(status().isBadRequest());
    }
    @Test void malformedJsonReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/customer/restock-subscriptions").header("Authorization","Bearer "+token(user(UserRole.CUSTOMER)))
            .contentType("application/json").content("{broken-json}")).andExpect(status().isBadRequest());
    }
}
