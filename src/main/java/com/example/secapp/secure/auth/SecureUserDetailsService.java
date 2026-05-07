package com.example.secapp.secure.auth;

import com.example.secapp.common.entity.User;
import com.example.secapp.secure.dao.SecureUserDao;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Secure 版の {@link UserDetailsService} 実装。
 * <p>
 * {@link SecureUserDao} からユーザを読み込み、{@link SecurePrincipal} として返す。
 */
@Service
@RequiredArgsConstructor
public class SecureUserDetailsService implements UserDetailsService {

    private final SecureUserDao userDao;

    /**
     * ログイン名（ユーザー名）から {@link UserDetails} を構築する。
     *
     * @param username ログインフォームのユーザー名
     * @return 認証に使う {@link SecurePrincipal}
     * @throws UsernameNotFoundException ユーザが存在しない場合
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + username));
        return new SecurePrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
