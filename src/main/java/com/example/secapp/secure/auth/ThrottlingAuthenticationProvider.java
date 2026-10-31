package com.example.secapp.secure.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * パスワード照合の前後で {@link LoginAttemptService} を呼び、ログイン試行回数を制限する {@link AuthenticationProvider}。
 * <p>
 * ロック中は、パスワードが正しくても照合せずに {@link LockedException} を投げる。
 * 実際の照合は委譲先（BCrypt を使う {@code DaoAuthenticationProvider}）が行う。
 */
@RequiredArgsConstructor
public class ThrottlingAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider delegate;
    private final LoginAttemptService loginAttemptService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        if (loginAttemptService.isLocked(username)) {
            throw new LockedException("too many failed login attempts");
        }
        try {
            Authentication result = delegate.authenticate(authentication);
            loginAttemptService.reset(username);
            return result;
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(username);
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}
