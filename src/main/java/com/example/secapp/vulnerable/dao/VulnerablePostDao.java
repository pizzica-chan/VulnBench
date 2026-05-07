package com.example.secapp.vulnerable.dao;

import com.example.secapp.common.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Vulnerable 版の投稿 DAO。
 * <p>
 * 一覧・検索・更新などすべて<strong>故意に</strong> SQL 文字列連結で実装する。
 */
@Repository
@RequiredArgsConstructor
public class VulnerablePostDao {

    private final JdbcTemplate jdbc;

    private static final String SELECT_BASE =
            "SELECT p.id, p.user_id, p.title, p.content, p.created_at, u.username AS author_name "
                    + "FROM vuln_posts p JOIN vuln_users u ON u.id = p.user_id ";

    private static final RowMapper<Post> ROW_MAPPER = (rs, n) -> Post.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .authorName(rs.getString("author_name"))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /**
     * 投稿を投稿 ID の降順で一覧取得する。
     *
     * @return 投稿のリスト
     */
    public List<Post> findAll() {
        return jdbc.query(SELECT_BASE + "ORDER BY p.id DESC", ROW_MAPPER);
    }

    /**
     * キーワードで投稿を検索する（危険: {@code LIKE} に直接連結）。
     *
     * @param keyword 検索語
     * @return ヒットした投稿リスト
     */
    public List<Post> search(String keyword) {
        String sql = SELECT_BASE
                + "WHERE p.title LIKE '%" + keyword + "%' "
                + "OR p.content LIKE '%" + keyword + "%' "
                + "ORDER BY p.id DESC";
        return jdbc.query(sql, ROW_MAPPER);
    }

    /**
     * 主キーで投稿を 1 件取得する。
     *
     * @param id 投稿 ID
     * @return 該当があれば {@link Optional}
     */
    public Optional<Post> findById(Long id) {
        String sql = SELECT_BASE + "WHERE p.id = " + id;
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, ROW_MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 新規投稿を挿入し、MySQL が採番した投稿 ID を返す。
     *
     * @param userId  投稿者ユーザー ID
     * @param title   タイトル
     * @param content 本文
     * @return 採番された投稿 ID（{@code LAST_INSERT_ID()}）
     */
    public Long create(Long userId, String title, String content) {
        String sql = "INSERT INTO vuln_posts (user_id, title, content) VALUES ("
                + userId + ", '" + title + "', '" + content + "')";
        jdbc.update(sql);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /**
     * 投稿を更新する（危険: 入力連結）。
     *
     * @param id      投稿 ID
     * @param title   新タイトル
     * @param content 新本文
     */
    public void update(Long id, String title, String content) {
        String sql = "UPDATE vuln_posts SET title = '" + title + "', content = '" + content + "' "
                + "WHERE id = " + id;
        jdbc.update(sql);
    }

    /**
     * 投稿および紐づくコメントを削除する。
     *
     * @param id 投稿 ID
     */
    public void delete(Long id) {
        jdbc.update("DELETE FROM vuln_comments WHERE post_id = " + id);
        jdbc.update("DELETE FROM vuln_posts WHERE id = " + id);
    }
}
