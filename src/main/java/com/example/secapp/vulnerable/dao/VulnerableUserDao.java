package com.example.secapp.vulnerable.dao;

import com.example.secapp.common.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Vulnerable 版のユーザ DAO。
 * <p>
 * SQL インジェクションを学ぶため<strong>故意に</strong>文字列連結で SQL を組み立てる。本番では決して利用しないこと。
 */
@Repository
@RequiredArgsConstructor
public class VulnerableUserDao {

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
     * ユーザー名・パスワードでユーザを検索する（危険: 値が SQL に直接連結される）。
     *
     * @param username ユーザー名
     * @param password パスワード（平文）
     * @return 一致したユーザがいればそれをラップした {@link Optional}
     */
    public Optional<User> findForLogin(String username, String password) {
        String sql = "SELECT * FROM vuln_users "
                + "WHERE username = '" + username + "' "
                + "AND password = '" + password + "'";
        try {
            User u = jdbc.queryForObject(sql, ROW_MAPPER);
            return Optional.ofNullable(u);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 主キーでユーザを取得する（危険: ID が SQL に直接連結される）。
     *
     * @param id ユーザー ID
     * @return 該当行があれば {@link Optional}
     */
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM vuln_users WHERE id = " + id;
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * ユーザー名でユーザを検索する（危険: 値が SQL に直接連結される）。
     *
     * @param username ユーザー名
     * @return 該当ユーザがいれば {@link Optional}
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM vuln_users WHERE username = '" + username + "'";
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * すべてのユーザを ID 昇順で取得する。
     *
     * @return ユーザのリスト（空リストの可能性あり）
     */
    public List<User> findAll() {
        return jdbc.query("SELECT * FROM vuln_users ORDER BY id", ROW_MAPPER);
    }

    /**
     * 新規ユーザを登録する（危険: パスワードを平文のまま INSERT し、入力も連結）。
     *
     * @param username       ユーザー名
     * @param plainPassword  平文パスワード
     * @param email          メールアドレス
     */
    public void create(String username, String plainPassword, String email) {
        String sql = "INSERT INTO vuln_users (username, password, email, role) VALUES ("
                + "'" + username + "', "
                + "'" + plainPassword + "', "
                + "'" + email + "', "
                + "'USER')";
        jdbc.update(sql);
    }

    /**
     * ユーザのパスワードを平文のまま更新する。
     *
     * @param id               ユーザー ID
     * @param newPlainPassword 新しい平文パスワード
     */
    public void updatePassword(Long id, String newPlainPassword) {
        String sql = "UPDATE vuln_users SET password = '" + newPlainPassword + "' WHERE id = " + id;
        jdbc.update(sql);
    }

    /**
     * ユーザのメールアドレスを更新する（危険: 値が連結）。
     *
     * @param id    ユーザー ID
     * @param email メールアドレス
     */
    public void updateEmail(Long id, String email) {
        String sql = "UPDATE vuln_users SET email = '" + email + "' WHERE id = " + id;
        jdbc.update(sql);
    }
}
