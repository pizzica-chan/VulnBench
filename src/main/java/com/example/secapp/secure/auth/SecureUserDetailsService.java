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
import java.util.Set;

/**
 * Secure 版の {@link UserDetailsService} 実装。
 * <p>
 * {@link SecureUserDao} からユーザを読み込み、{@link SecurePrincipal} として返す。
 */
@Service
@RequiredArgsConstructor
public class SecureUserDetailsService implements UserDetailsService {

    private final SecureUserDao userDao;

    /** 教材で許可するロールのホワイトリスト。これ以外の DB 値は USER として扱う。 */
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "USER");

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
                List.of(new SimpleGrantedAuthority("ROLE_" + normalizeRole(user.getRole())))
        );
    }

    /**
     * DB 上のロール文字列をホワイトリストで正規化する。
     * <p>
     * 想定外（{@code null} / 小文字 / typo / 空白混入）はすべて {@code USER} に丸める。
     * これにより不整合な権限名が伝播してアクセス制御が壊れることを防ぐ。
     *
     * @param raw DB のロール値
     * @return 正規化したロール（{@code "ADMIN"} or {@code "USER"}）
     */
    private static String normalizeRole(String raw) {
        if (raw == null) return "USER";
        String upper = raw.trim().toUpperCase();
        return ALLOWED_ROLES.contains(upper) ? upper : "USER";
    }
}
