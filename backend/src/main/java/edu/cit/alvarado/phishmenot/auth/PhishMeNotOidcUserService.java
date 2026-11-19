package edu.cit.alvarado.phishmenot.auth;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import edu.cit.alvarado.phishmenot.user.User;

/**
 * Loads the Google user, registers/updates the matching PhishMeNot account, and grants the authority
 * stored in the database (ROLE_USER or ROLE_ADMIN). Google-provided scopes are not used as authorities.
 */
@Service
public class PhishMeNotOidcUserService extends OidcUserService {

    private final UserAccountService userAccountService;

    public PhishMeNotOidcUserService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser googleUser = super.loadUser(userRequest);
        User account = userAccountService.registerOrLogin(GoogleProfile.from(googleUser));
        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority(account.getRole().authority())),
                googleUser.getIdToken(),
                googleUser.getUserInfo(),
                IdTokenClaimNames.SUB);
    }
}
