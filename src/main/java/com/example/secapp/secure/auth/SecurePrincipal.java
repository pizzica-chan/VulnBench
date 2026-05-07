package com.example.secapp.secure.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Spring Security の {@link User} を拡張し、データベース上のユーザー主キー {@link #userId} を保持する。
 * <p>
 * 投稿・プロフィールのオーナー判定に利用する。
 */
@Getter
public class SecurePrincipal extends User {

    private final Long userId;

    /**
     * ユーザー名・パスワード（ハッシュ）・権限とともに、アプリ側のユーザー ID を束ねて構築する。
     *
     * @param userId      アプリ独自のユーザー主キー
     * @param username    ユーザー名（Spring Security がログイン入力と照合）
     * @param password    BCrypt 化済みパスワード文字列（セキュリティフレームワーク向け）
     * @param authorities 付与する権限集合
     */
    public SecurePrincipal(Long userId,
                           String username,
                           String password,
                           Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }
}
