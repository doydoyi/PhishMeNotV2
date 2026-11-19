package edu.cit.alvarado.phishmenot.config;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.cit.alvarado.phishmenot.auth.LoginFailureHandler;
import edu.cit.alvarado.phishmenot.auth.LoginSuccessHandler;
import edu.cit.alvarado.phishmenot.auth.PhishMeNotOidcUserService;

/**
 * Authentication and role-based access control (FR-020, FR-026, FR-039, NFR-005, NFR-006).
 *
 * <ul>
 *   <li>Sign-in: Google OAuth 2.0 / OpenID Connect via {@code /oauth2/authorization/google}.</li>
 *   <li>Session: server-side HTTP session, identified by an HttpOnly, SameSite=Lax cookie.</li>
 *   <li>Sign-out: {@code POST /api/auth/logout} invalidates the session and deletes the cookie.</li>
 *   <li>CSRF: double-submit cookie {@code XSRF-TOKEN} / header {@code X-XSRF-TOKEN} for the SPA.</li>
 *   <li>Unauthenticated API calls get 401 JSON; authenticated calls without the right role get 403.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            PhishMeNotOidcUserService oidcUserService,
            LoginSuccessHandler successHandler,
            LoginFailureHandler failureHandler,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        http
            .cors(cors -> {})
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll())
            .oauth2Login(oauth -> oauth
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestResolver(accountChooserResolver(clientRegistrationRepository)))
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                    .successHandler(successHandler)
                    .failureHandler(failureHandler))
            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .sessionManagement(session -> session
                    .sessionFixation(fixation -> fixation.changeSessionId()))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, e) ->
                            writeError(response, HttpStatus.UNAUTHORIZED, "Please sign in to continue.", request))
                    .accessDeniedHandler((request, response, e) ->
                            writeError(response, HttpStatus.FORBIDDEN,
                                    "You do not have permission to access this resource.", request)));

        return http.build();
    }

    /**
     * Adds {@code prompt=select_account} so Google always shows its account chooser. Without it, a browser
     * that is still signed in to Google is logged straight back into the same account after a PhishMeNot
     * sign-out, and the user cannot switch accounts.
     */
    private static OAuth2AuthorizationRequestResolver accountChooserResolver(ClientRegistrationRepository repository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(request ->
                request.additionalParameters(params -> params.put("prompt", "select_account")));
        return resolver;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(appProperties.frontendUrl()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message,
            HttpServletRequest request) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":%d,"error":"%s","message":"%s","path":"%s","timestamp":"%s"}"""
                .formatted(status.value(), status.getReasonPhrase(), message,
                        request.getRequestURI().replace("\\", "").replace("\"", ""), Instant.now()));
    }

    /**
     * CSRF handling for a single-page app, as described in the Spring Security reference: BREACH-protected
     * tokens when rendered, plain token accepted from the {@code X-XSRF-TOKEN} header.
     */
    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return StringUtils.hasText(headerValue)
                    ? plain.resolveCsrfTokenValue(request, csrfToken)
                    : xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    /** Makes sure the XSRF-TOKEN cookie is written on every response so the SPA can read it. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
