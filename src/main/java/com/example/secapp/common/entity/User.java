package com.example.secapp.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * アプリケーション上のユーザを表すドメインオブジェクト。
 * <p>
 * Vulnerable 側では {@code password} に平文を、Secure 側では BCrypt ハッシュを格納する。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    /** Vulnerable 側は平文、Secure 側は BCrypt ハッシュ。 */
    private String password;
    private String email;
    private String role;
    private LocalDateTime createdAt;

    /**
     * ログやデバッグ用の短い文字列表現。パスワードは内容を伏せる。
     *
     * @return マスク済みの要約文字列
     */
    @Override
    public String toString() {
        return "User(id=" + id + ", username=" + username + ", password=[PROTECTED], email=" + email
                + ", role=" + role + ", createdAt=" + createdAt + ")";
    }
}
