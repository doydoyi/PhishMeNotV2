package edu.cit.alvarado.phishmenot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import edu.cit.alvarado.phishmenot.auth.AccountRegistrationException;
import edu.cit.alvarado.phishmenot.auth.GoogleProfile;
import edu.cit.alvarado.phishmenot.auth.LoginFailureHandler;
import edu.cit.alvarado.phishmenot.auth.LoginSuccessHandler;
import edu.cit.alvarado.phishmenot.auth.UserAccountService;
import edu.cit.alvarado.phishmenot.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired UserAccountService accounts;
    @Autowired LoginSuccessHandler successHandler;
    @Autowired LoginFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        accounts.registerOrLogin(new GoogleProfile("g-user", "user@example.com", true, "Regular User", null));
        accounts.registerOrLogin(new GoogleProfile("g-admin", "boss@example.com", true, "Admin User", null));
    }

    private static RequestPostProcessor asUser() {
        return oidcLogin().idToken(t -> t.subject("g-user")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor asAdmin() {
        return oidcLogin().idToken(t -> t.subject("g-admin")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    @WithAnonymousUser
    void anonymousRequestsToProtectedEndpointsGet401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please sign in to continue."));
        mvc.perform(get("/api/user/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void signInStartsGoogleOAuthFlow() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.startsWith("https://accounts.google.com/o/oauth2/v2/auth"),
                                org.hamcrest.Matchers.containsString("prompt=select_account"))));
    }

    @Test
    void meReturnsProfileAndRoleFromDatabase() throws Exception {
        mvc.perform(get("/api/me").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.googleId").doesNotExist());
    }

    @Test
    void sessionForDeletedAccountIsRejected() throws Exception {
        mvc.perform(get("/api/me").with(oidcLogin().idToken(t -> t.subject("g-ghost"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generalUserCanOpenUserDashboardButNotAdmin() throws Exception {
        mvc.perform(get("/api/user/dashboard").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.name").value("Regular User"));
        mvc.perform(get("/api/admin/dashboard").with(asUser())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(asUser())).andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenAdminDashboardAndListUsers() throws Exception {
        mvc.perform(get("/api/admin/dashboard").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.administrators").value(1))
                .andExpect(jsonPath("$.generalUsers").value(1));
        mvc.perform(get("/api/admin/users").param("query", "regular").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.users[0].email").value("user@example.com"));
    }

    @Test
    void logoutRequiresCsrfTokenAndEndsSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/logout").session(session).with(asUser()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/logout").session(session).with(asUser()).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void successHandlerRedirectsByRole() throws Exception {
        MockHttpServletResponse userResponse = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), userResponse,
                new TestingAuthenticationToken("u", null, "ROLE_USER"));
        assertThat(userResponse.getRedirectedUrl()).isEqualTo("http://localhost:5173/dashboard");

        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), adminResponse,
                new TestingAuthenticationToken("a", null, "ROLE_ADMIN"));
        assertThat(adminResponse.getRedirectedUrl()).isEqualTo("http://localhost:5173/admin");
    }

    @Test
    void failureHandlerSendsFriendlyErrorCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        failureHandler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new AccountRegistrationException(AccountRegistrationException.ACCOUNT_CONFLICT, "dup"));
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=account_conflict");

        MockHttpServletResponse other = new MockHttpServletResponse();
        failureHandler.onAuthenticationFailure(new MockHttpServletRequest(), other,
                new OAuth2AuthenticationException(new OAuth2Error("invalid_token_response")));
        assertThat(other.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=login_failed");
    }
}
