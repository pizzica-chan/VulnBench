package com.example.secapp.vulnerable.dao;

import com.example.secapp.common.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Vulnerable 版のコメント DAO。
 * <p>
 * クエリ構築が文字列連結ベースの教材実装である。
 */
@Repository
@RequiredArgsConstructor
public class VulnerableCommentDao {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Comment> ROW_MAPPER = (rs, n) -> Comment.builder()
            .id(rs.getLong("id"))
            .postId(rs.getLong("post_id"))
            .userId(rs.getLong("user_id"))
            .authorName(rs.getString("author_name"))
            .content(rs.getString("content"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /**
     * 指定投稿に紐づくコメントを ID 昇順で返す。
     *
     * @param postId 投稿 ID
     * @return コメントリスト
     */
    public List<Comment> findByPostId(Long postId) {
        String sql = "SELECT c.id, c.post_id, c.user_id, c.content, c.created_at, u.username AS author_name "
                + "FROM vuln_comments c JOIN vuln_users u ON u.id = c.user_id "
                + "WHERE c.post_id = " + postId + " ORDER BY c.id";
        return jdbc.query(sql, ROW_MAPPER);
    }

    /**
     * コメントを新規作成する。
     *
     * @param postId  対象投稿 ID
     * @param userId  投稿者ユーザー ID
     * @param content 本文
     */
    public void create(Long postId, Long userId, String content) {
        String sql = "INSERT INTO vuln_comments (post_id, user_id, content) VALUES ("
                + postId + ", " + userId + ", '" + content + "')";
        jdbc.update(sql);
    }
}
