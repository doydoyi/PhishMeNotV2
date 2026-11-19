package edu.cit.alvarado.phishmenot.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import edu.cit.alvarado.phishmenot.config.AppProperties;
import edu.cit.alvarado.phishmenot.user.Role;

/** After a successful Google sign-in, sends each user to the dashboard for their role. */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AppProperties appProperties;

    public LoginSuccessHandler(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, appProperties.frontendUrl() + dashboardPath(authentication));
    }

    static String dashboardPath(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> Role.ADMIN.authority().equals(a.getAuthority()));
        return admin ? "/admin" : "/dashboard";
    }
}
