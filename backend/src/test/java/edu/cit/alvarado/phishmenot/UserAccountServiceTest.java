package edu.cit.alvarado.phishmenot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import edu.cit.alvarado.phishmenot.auth.AccountRegistrationException;
import edu.cit.alvarado.phishmenot.auth.GoogleProfile;
import edu.cit.alvarado.phishmenot.auth.UserAccountService;
import edu.cit.alvarado.phishmenot.user.Role;
import edu.cit.alvarado.phishmenot.user.User;
import edu.cit.alvarado.phishmenot.user.UserRepository;

@SpringBootTest
class UserAccountServiceTest {

    @Autowired UserAccountService service;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    private static GoogleProfile profile(String sub, String email) {
        return new GoogleProfile(sub, email, true, "Juan Dela Cruz", "https://lh3.googleusercontent.com/a/pic");
    }

    @Test
    void firstSignInCreatesGeneralUserAccount() {
        User user = service.registerOrLogin(profile("g-100", "juan@example.com"));

        assertThat(user.getUserId()).isNotNull();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(userRepository.findByGoogleId("g-100")).isPresent();
    }

    @Test
    void returningUserIsUpdatedNotDuplicated() {
        User first = service.registerOrLogin(profile("g-100", "juan@example.com"));
        User again = service.registerOrLogin(
                new GoogleProfile("g-100", "juan@example.com", true, "Juan D. Cruz", null));

        assertThat(again.getUserId()).isEqualTo(first.getUserId());
        assertThat(again.getName()).isEqualTo("Juan D. Cruz");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void sameEmailWithDifferentGoogleAccountIsRejected() {
        service.registerOrLogin(profile("g-100", "juan@example.com"));

        assertThatThrownBy(() -> service.registerOrLogin(profile("g-999", "JUAN@example.com")))
                .isInstanceOf(AccountRegistrationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(AccountRegistrationException.ACCOUNT_CONFLICT);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void unverifiedEmailIsRejected() {
        assertThatThrownBy(() -> service.registerOrLogin(
                new GoogleProfile("g-1", "x@example.com", false, "X", null)))
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(AccountRegistrationException.EMAIL_NOT_VERIFIED);
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void missingRequiredFieldsAreRejected() {
        assertThatThrownBy(() -> service.registerOrLogin(new GoogleProfile("g-1", null, true, "X", null)))
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(AccountRegistrationException.INVALID_PROFILE);
        assertThatThrownBy(() -> service.registerOrLogin(new GoogleProfile(" ", "a@example.com", true, "X", null)))
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(AccountRegistrationException.INVALID_PROFILE);
        assertThatThrownBy(() -> service.registerOrLogin(new GoogleProfile("g-1", "not-an-email", true, "X", null)))
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(AccountRegistrationException.INVALID_PROFILE);
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void configuredAdminEmailGetsAdminRole() {
        User admin = service.registerOrLogin(profile("g-admin", "boss@example.com"));
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isAdmin()).isTrue();
    }

    @Test
    void existingUserListedAsAdminIsPromotedOnNextSignIn() {
        // boss@example.com is in app.admin-emails; simulate an account created before it was listed
        User user = service.registerOrLogin(profile("g-late", "late@example.com"));
        assertThat(user.getRole()).isEqualTo(Role.USER);

        User again = service.registerOrLogin(new GoogleProfile("g-boss", "boss@example.com", true, "Boss", null));
        assertThat(again.getRole()).isEqualTo(Role.ADMIN);

        userRepository.deleteAll();
        User plain = userRepository.saveAndFlush(new User("g-boss", "boss@example.com", "Boss", null, Role.USER));
        User promoted = service.registerOrLogin(new GoogleProfile("g-boss", "boss@example.com", true, "Boss", null));
        assertThat(promoted.getUserId()).isEqualTo(plain.getUserId());
        assertThat(promoted.getRole()).isEqualTo(Role.ADMIN);
    }
}
