package edu.cit.alvarado.phishmenot.user;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.alvarado.phishmenot.auth.CurrentUserService;

/** General User dashboard data. Reachable by USER and ADMIN (an administrator inherits user use cases). */
@RestController
@RequestMapping("/api/user")
public class UserDashboardController {

    private final CurrentUserService currentUserService;

    public UserDashboardController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public record UserDashboardDto(UserDto account, long daysSinceRegistration) {}

    @GetMapping("/dashboard")
    public UserDashboardDto dashboard(Authentication authentication) {
        User user = currentUserService.require(authentication);
        long days = ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate());
        return new UserDashboardDto(UserDto.from(user), days);
    }
}
