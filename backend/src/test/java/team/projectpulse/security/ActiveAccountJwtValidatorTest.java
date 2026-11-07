package team.projectpulse.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.user.UserRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * FR-SEC-active-account at the unit level: the validator admits a token only while the account it names is
 * active, and fails closed on every claim it cannot resolve.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActiveAccountJwtValidator")
class ActiveAccountJwtValidatorTest {

    @Mock
    ObjectProvider<UserRepository> userRepositoryProvider;

    @Mock
    UserRepository userRepository;

    ActiveAccountJwtValidator validator;


    @BeforeEach
    void setUp() {
        this.validator = new ActiveAccountJwtValidator(this.userRepositoryProvider);
    }

    private static Jwt tokenWithUserId(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (userId != null) {
            builder.claim("userId", userId);
        } else {
            builder.claim("sub", "someone");
        }
        return builder.build();
    }

    @Test
    void anActiveAccountIsAdmitted() {
        given(this.userRepositoryProvider.getObject()).willReturn(this.userRepository);
        given(this.userRepository.existsByIdAndEnabledTrue(1)).willReturn(true);

        assertThat(this.validator.validate(tokenWithUserId(1L)).hasErrors()).isFalse();
    }

    @Test
    void aDeactivatedAccountIsRefused() {
        given(this.userRepositoryProvider.getObject()).willReturn(this.userRepository);
        given(this.userRepository.existsByIdAndEnabledTrue(1)).willReturn(false); // Deactivated

        assertThat(this.validator.validate(tokenWithUserId(1L)).hasErrors()).isTrue();
    }

    @Test
    void anIdNamingNoAccountIsRefused() {
        given(this.userRepositoryProvider.getObject()).willReturn(this.userRepository);
        given(this.userRepository.existsByIdAndEnabledTrue(999999)).willReturn(false); // No such account

        assertThat(this.validator.validate(tokenWithUserId(999999)).hasErrors()).isTrue();
    }

    @Test
    void aMissingOrMalformedClaimIsRefusedWithoutAQuery() {
        assertThat(this.validator.validate(tokenWithUserId(null)).hasErrors()).isTrue();
        assertThat(this.validator.validate(tokenWithUserId("not-a-number")).hasErrors()).isTrue();
        verifyNoInteractions(this.userRepositoryProvider);
    }

}
