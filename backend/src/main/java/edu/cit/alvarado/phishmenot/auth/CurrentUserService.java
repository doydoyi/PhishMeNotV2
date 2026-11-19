package edu.cit.alvarado.phishmenot.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import edu.cit.alvarado.phishmenot.common.UnauthenticatedException;
import edu.cit.alvarado.phishmenot.user.User;
import edu.cit.alvarado.phishmenot.user.UserRepository;

/** Resolves the signed-in principal to its PhishMeNot account in the database. */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User principal)) {
            throw new UnauthenticatedException();
        }
        return userRepository.findByGoogleId(principal.getName())
                .orElseThrow(UnauthenticatedException::new);
    }
}
