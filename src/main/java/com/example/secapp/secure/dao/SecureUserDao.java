package com.example.secapp.secure.dao;

import com.example.secapp.common.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Secure 版のユーザ DAO。
 * <p>
 * すべてパラメータ化クエリにより SQL インジェクションを防ぐ。パスワード列は BCrypt ハッシュのみを扱う。
 */
@Repository
@RequiredArgsConstructor
public class SecureUserDao {

    private final JdbcTemplate jdbc;

    private static final RowMapper<User> ROW_MAPPER = (rs, n) -> User.builder()
            .id(rs.getLong("id"))
            .username(rs.getString("username"))
            .password(rs.getString("password"))
            .email(rs.getString("email"))
            .role(rs.getString("role"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /**
     * 主キーでユーザを取得する。
     *
     * @param id ユーザー ID
     * @return 該当があれば {@link Optional}
     */
    public Optional<User> findById(Long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT * FROM sec_users WHERE id = ?", ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * ユーザー名でユーザを取得する（認証処理の入力に使う）。
     *
     * @param username ユーザー名
     * @return 該当があれば {@link Optional}
     */
    public Optional<User> findByUsername(String username) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT * FROM sec_users WHERE username = ?", ROW_MAPPER, username));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * すべてのユーザを ID 昇順で取得する。
     *
     * @return ユーザリスト
     */
    public List<User> findAll() {
        return jdbc.query("SELECT * FROM sec_users ORDER BY id", ROW_MAPPER);
    }

    /**
     * 新規ユーザを登録する。{@code password} は呼び出し側で BCrypt ハッシュ化済みであること。
     *
     * @param username       ユーザー名
     * @param hashedPassword BCrypt ハッシュ文字列
     * @param email          メール
     * @param role           役割（{@code ADMIN} / {@code USER} など）
     */
    public void create(String username, String hashedPassword, String email, String role) {
        jdbc.update(
                "INSERT INTO sec_users (username, password, email, role) VALUES (?, ?, ?, ?)",
                username, hashedPassword, email, role);
    }

    /**
     * ハッシュ済みパスワードで更新する。
     *
     * @param id               ユーザー ID
     * @param hashedPassword   新 BCrypt ハッシュ
     */
    public void updatePassword(Long id, String hashedPassword) {
        jdbc.update("UPDATE sec_users SET password = ? WHERE id = ?", hashedPassword, id);
    }

    /**
     * メールアドレスを更新する。
     *
     * @param id    ユーザー ID
     * @param email 新メール
     */
    public void updateEmail(Long id, String email) {
        jdbc.update("UPDATE sec_users SET email = ? WHERE id = ?", email, id);
    }
}
