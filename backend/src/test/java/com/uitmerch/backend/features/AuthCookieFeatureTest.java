package com.uitmerch.backend.features;

import com.uitmerch.backend.auth.service.AuthCookieService;
import com.uitmerch.backend.common.model.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class AuthCookieFeatureTest extends BackendFeatureTest {
    private Cookie csrf() throws Exception {
        var response=mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        return new Cookie(AuthCookieService.CSRF,json.readTree(response.getContentAsString()).path("data").path("csrfToken").asText());
    }
    private Cookie refreshCookie(MockHttpServletResponse response) {
        String header=response.getHeaders("Set-Cookie").stream().filter(s->s.startsWith(AuthCookieService.REFRESH+"=")).findFirst().orElseThrow();
        assertThat(header).contains("HttpOnly","Secure","SameSite=Lax","Path=/api/v1/auth").doesNotContain("Domain=");
        return new Cookie(AuthCookieService.REFRESH,header.substring(header.indexOf('=')+1,header.indexOf(';')));
    }
    @Test void loginRotationAndLogoutKeepRefreshCredentialsOutOfJsonAndRevokeAccess() throws Exception {
        var customer=user(UserRole.CUSTOMER);Cookie csrf=csrf();
        var login=mvc.perform(post("/api/v1/auth/login").cookie(csrf).header("X-CSRF-TOKEN",csrf.getValue())
            .contentType("application/json").content(json.writeValueAsString(Map.of("email",customer.getEmail(),"password","Password1"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        Cookie old=refreshCookie(login);
        var rotated=mvc.perform(post("/api/v1/auth/refresh").cookie(csrf,old).header("X-CSRF-TOKEN",csrf.getValue()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.refreshToken").doesNotExist()).andReturn().getResponse();
        Cookie fresh=refreshCookie(rotated);assertThat(fresh.getValue()).isNotEqualTo(old.getValue());
        mvc.perform(post("/api/v1/auth/refresh").cookie(csrf,old).header("X-CSRF-TOKEN",csrf.getValue())).andExpect(status().isUnauthorized());
        String access=json.readTree(rotated.getContentAsString()).path("data").path("token").asText();
        mvc.perform(post("/api/v1/auth/logout").cookie(csrf,fresh).header("X-CSRF-TOKEN",csrf.getValue()))
            .andExpect(status().isOk()).andExpect(header().stringValues("Set-Cookie",org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("Max-Age=0"))));
        mvc.perform(get("/api/v1/customer/orders").header("Authorization","Bearer "+access)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(csrf,fresh).header("X-CSRF-TOKEN",csrf.getValue())).andExpect(status().isUnauthorized());
    }
    @Test void csrfOriginSignatureAndCookieAreRequiredForSessionMutations() throws Exception {
        Cookie csrf=csrf();
        for(String path:new String[]{"login","refresh","logout"}) {
            String route="/api/v1/auth/"+path;
            String body=path.equals("login")?"{\"email\":\"csrf@uit.edu.vn\",\"password\":\"Password1\"}":"{}";
            mvc.perform(post(route).contentType("application/json").content(body)).andExpect(status().isForbidden());
            mvc.perform(post(route).cookie(csrf).header("X-CSRF-TOKEN",csrf.getValue()).header("Origin","https://evil.example")
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
            Cookie forged=new Cookie(AuthCookieService.CSRF,csrf.getValue().substring(0,csrf.getValue().length()-2)+"xx");
            mvc.perform(post(route).cookie(forged).header("X-CSRF-TOKEN",forged.getValue()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/auth/refresh").cookie(csrf).header("X-CSRF-TOKEN",csrf.getValue())
            .contentType("application/json").content("{\"refreshToken\":\"legacy-body-token\"}")).andExpect(status().isUnauthorized());
    }
}
