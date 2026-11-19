package edu.cit.alvarado.phishmenot.auth;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import edu.cit.alvarado.phishmenot.config.AppProperties;

/**
 * Sends the browser back to the frontend login page with a short error code. Technical details are
 * logged on the server only (NFR-016).
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginFailureHandler.class);

    /** Codes the frontend knows how to explain; anything else is reported as a generic failure. */
    private static final Set<String> KNOWN_CODES = Set.of(
            "access_denied",
            AccountRegistrationException.INVALID_PROFILE,
            AccountRegistrationException.EMAIL_NOT_VERIFIED,
            AccountRegistrationException.ACCOUNT_CONFLICT);

    private final AppProperties appProperties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public LoginFailureHandler(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String code = "login_failed";
        if (exception instanceof OAuth2AuthenticationException oauth && KNOWN_CODES.contains(oauth.getError().getErrorCode())) {
            code = oauth.getError().getErrorCode();
        }
        log.warn("Google sign-in failed: code={} reason={}", code, exception.getMessage());
        String target = UriComponentsBuilder.fromUriString(appProperties.frontendUrl() + "/login")
                .queryParam("error", code)
                .build()
                .toUriString();
        redirectStrategy.sendRedirect(request, response, target);
    }
}
