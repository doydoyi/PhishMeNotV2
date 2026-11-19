package edu.cit.alvarado.phishmenot.auth;

import java.util.Locale;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The registration fields received from Google OAuth 2.0 (SRS 1.5, FR-021).
 * Bean Validation constraints mirror the USERS table columns.
 */
public record GoogleProfile(
        @NotBlank(message = "Google account id is missing")
        @Size(max = 64, message = "Google account id is too long")
        String googleId,

        @NotBlank(message = "Email address is missing")
        @Email(message = "Email address is not valid")
        @Size(max = 255, message = "Email address is too long")
        String email,

        @AssertTrue(message = "Google has not verified this email address")
        boolean emailVerified,

        @NotBlank(message = "Name is missing")
        @Size(max = 150, message = "Name is too long")
        String name,

        @Size(max = 500, message = "Profile picture URL is too long")
        String pictureUrl) {

    public static GoogleProfile from(OidcUser oidcUser) {
        String email = trimToNull(oidcUser.getEmail());
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }
        String name = trimToNull(oidcUser.getFullName());
        if (name == null) {
            name = trimToNull(oidcUser.getGivenName());
        }
        if (name == null && email != null && email.contains("@")) {
            name = email.substring(0, email.indexOf('@'));
        }
        String picture = trimToNull(oidcUser.getPicture());
        if (picture != null && (picture.length() > 500 || !picture.startsWith("https://"))) {
            picture = null; // optional field: drop values we would not safely store or display
        }
        return new GoogleProfile(
                trimToNull(oidcUser.getSubject()),
                email,
                Boolean.TRUE.equals(oidcUser.getEmailVerified()),
                name,
                picture);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
