package edu.cit.alvarado.phishmenot.auth;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.alvarado.phishmenot.config.AppProperties;
import edu.cit.alvarado.phishmenot.user.Role;
import edu.cit.alvarado.phishmenot.user.User;
import edu.cit.alvarado.phishmenot.user.UserRepository;

/**
 * Creates an account on a user's first Google sign-in and updates it on later sign-ins
 * (SRS Figure 7, steps 8-10; FR-020, FR-021).
 */
@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserRepository userRepository;
    private final Validator validator;
    private final AppProperties appProperties;

    public UserAccountService(UserRepository userRepository, Validator validator, AppProperties appProperties) {
        this.userRepository = userRepository;
        this.validator = validator;
        this.appProperties = appProperties;
    }

    /**
     * Validates the Google profile, then either registers a new account (role USER) or records the login
     * for a returning user.
     *
     * @throws AccountRegistrationException if required fields are missing/invalid, the e-mail is not
     *                                      verified by Google, or the e-mail already belongs to another account
     */
    @Transactional
    public User registerOrLogin(GoogleProfile profile) {
        validate(profile);

        LocalDateTime now = LocalDateTime.now();
        User existing = userRepository.findByGoogleId(profile.googleId()).orElse(null);
        if (existing != null) {
            ensureEmailNotTakenByAnotherAccount(profile.email(), existing.getUserId());
            existing.setEmail(profile.email());
            existing.setName(profile.name());
            existing.setProfilePictureUrl(profile.pictureUrl());
            existing.setLastLoginAt(now);
            if (!existing.isAdmin() && appProperties.isBootstrapAdmin(profile.email())) {
                existing.setRole(Role.ADMIN);
                log.info("Promoted account id={} to ADMIN (listed in app.admin-emails)", existing.getUserId());
            }
            return existing;
        }

        ensureEmailNotTakenByAnotherAccount(profile.email(), null);

        Role role = appProperties.isBootstrapAdmin(profile.email()) ? Role.ADMIN : Role.USER;
        User user = new User(profile.googleId(), profile.email(), profile.name(), profile.pictureUrl(), role);
        user.setLastLoginAt(user.getCreatedAt());
        try {
            User saved = userRepository.saveAndFlush(user);
            log.info("Registered new account id={} role={}", saved.getUserId(), saved.getRole());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Two sign-ins racing for the same Google account or e-mail: the unique keys reject the duplicate.
            throw new AccountRegistrationException(AccountRegistrationException.ACCOUNT_CONFLICT,
                    "An account with this Google identity or email already exists");
        }
    }

    private void validate(GoogleProfile profile) {
        Set<ConstraintViolation<GoogleProfile>> violations = validator.validate(profile);
        if (violations.isEmpty()) {
            return;
        }
        boolean onlyUnverified = violations.stream()
                .allMatch(v -> v.getPropertyPath().toString().equals("emailVerified"));
        String message = violations.stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        throw new AccountRegistrationException(
                onlyUnverified ? AccountRegistrationException.EMAIL_NOT_VERIFIED
                        : AccountRegistrationException.INVALID_PROFILE,
                message);
    }

    private void ensureEmailNotTakenByAnotherAccount(String email, Long ownUserId) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(other -> !other.getUserId().equals(ownUserId))
                .ifPresent(other -> {
                    throw new AccountRegistrationException(AccountRegistrationException.ACCOUNT_CONFLICT,
                            "This email address is already linked to a different account");
                });
    }
}
