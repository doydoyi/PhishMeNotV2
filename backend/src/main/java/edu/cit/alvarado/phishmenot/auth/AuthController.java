package edu.cit.alvarado.phishmenot.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.alvarado.phishmenot.user.UserDto;

/**
 * Session endpoints. Sign-in starts at {@code GET /oauth2/authorization/google} and sign-out is
 * {@code POST /api/auth/logout}; both are handled by Spring Security (see SecurityConfig).
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final CurrentUserService currentUserService;

    public AuthController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    /** Profile and role of the signed-in user (SRS Figure 7, step 12). */
    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return UserDto.from(currentUserService.require(authentication));
    }
}
