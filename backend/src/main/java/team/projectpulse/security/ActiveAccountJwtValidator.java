package team.projectpulse.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a bearer token whose account is deactivated, so a session already signed in when the account is
 * deactivated loses access on its next request rather than when its token expires (FR-SEC-active-account).
 *
 * <p>It runs on every bearer request, after the signature and expiry validators, and reads {@code enabled} from
 * the database each time. There is deliberately no cache: a cache would reopen the window this check closes
 * (docs/design/stu.md, D2). It fails closed, so a token with no {@code userId} claim, a claim that is not a
 * number, or a claim naming no account is refused too.
 *
 * <p>The repository is reached through an {@link ObjectProvider} and resolved on each call, so building the
 * {@code JwtDecoder} bean does not pull JPA into {@code SecurityConfiguration}'s construction.
 */
@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INACTIVE_ACCOUNT = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
            "The account this token was issued to is deactivated or does not exist.", null);

    private final ObjectProvider<UserRepository> userRepositoryProvider;


    public ActiveAccountJwtValidator(ObjectProvider<UserRepository> userRepositoryProvider) {
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Integer userId = readUserId(jwt.getClaim("userId"));
        if (userId == null) {
            return OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
        }
        boolean active = this.userRepositoryProvider.getObject().findById(userId)
                .map(PeerEvaluationUser::isEnabled)
                .orElse(false);
        return active ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
    }

    private static Integer readUserId(Object claim) {
        if (claim instanceof Number number) {
            return number.intValue();
        }
        if (claim instanceof String text) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
