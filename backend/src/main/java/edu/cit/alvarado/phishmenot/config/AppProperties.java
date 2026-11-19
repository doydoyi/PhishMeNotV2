package edu.cit.alvarado.phishmenot.config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings bound from {@code app.*} properties (set through environment variables).
 *
 * @param frontendUrl  base URL of the React frontend; used for post-login redirects and CORS
 * @param adminEmails  optional list of Google e-mail addresses that receive the ADMIN role when they sign in
 *                     (new or existing account). FR-046: roles are otherwise changed directly in the database.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String frontendUrl, List<String> adminEmails) {

    public AppProperties {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            frontendUrl = "http://localhost:5173";
        }
        frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        adminEmails = adminEmails == null ? List.of()
                : adminEmails.stream()
                        .filter(e -> e != null && !e.isBlank())
                        .map(e -> e.trim().toLowerCase(Locale.ROOT))
                        .toList();
    }

    public boolean isBootstrapAdmin(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
