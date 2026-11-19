package edu.cit.alvarado.phishmenot.auth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Raised while creating or loading an account during Google sign-in. The error code is passed to the
 * frontend login page (as {@code ?error=<code>}) so it can show a friendly message.
 */
public class AccountRegistrationException extends OAuth2AuthenticationException {

    public static final String INVALID_PROFILE = "invalid_profile";
    public static final String EMAIL_NOT_VERIFIED = "email_not_verified";
    public static final String ACCOUNT_CONFLICT = "account_conflict";

    public AccountRegistrationException(String code, String description) {
        super(new OAuth2Error(code, description, null), description);
    }
}
