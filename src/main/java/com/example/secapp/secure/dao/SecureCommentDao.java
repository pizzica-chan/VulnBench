package com.example.secapp.secure.dao;

import com.example.secapp.common.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Secure 版のコメント DAO。
 */
@Repository
@RequiredArgsConstructor
public class SecureCommentDao {

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
     * 指定投稿のコメントを ID 昇順で返す。
     *
     * @param postId 投稿 ID
     * @return コメントリスト
     */
    public List<Comment> findByPostId(Long postId) {
        return jdbc.query(
                "SELECT c.id, c.post_id, c.user_id, c.content, c.created_at, u.username AS author_name "
                        + "FROM sec_comments c JOIN sec_users u ON u.id = c.user_id "
                        + "WHERE c.post_id = ? ORDER BY c.id",
                ROW_MAPPER, postId);
    }

    /**
     * コメントを追加する。
     *
     * @param postId  投稿 ID
     * @param userId  投稿者ユーザー ID
     * @param content 本文
     */
    public void create(Long postId, Long userId, String content) {
        jdbc.update(
                "INSERT INTO sec_comments (post_id, user_id, content) VALUES (?, ?, ?)",
                postId, userId, content);
    }

    /**
     * 主キーでコメントを 1 件取得する（プレースホルダ）。
     *
     * @param id コメント ID
     * @return 該当があれば {@link Optional}
     */
    public Optional<Comment> findById(Long id) {
        try {
            Comment c = jdbc.queryForObject(
                    "SELECT c.id, c.post_id, c.user_id, c.content, c.created_at, u.username AS author_name "
                            + "FROM sec_comments c JOIN sec_users u ON u.id = c.user_id "
                            + "WHERE c.id = ?",
                    ROW_MAPPER, id);
            return Optional.ofNullable(c);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * コメント本文を更新する（プレースホルダ）。認可はサービス層で行う前提。
     *
     * @param id      コメント ID
     * @param content 新しい本文
     */
    public void update(Long id, String content) {
        jdbc.update("UPDATE sec_comments SET content = ? WHERE id = ?", content, id);
    }

    /**
     * コメントを削除する（プレースホルダ）。認可はサービス層で行う前提。
     *
     * @param id コメント ID
     */
    public void delete(Long id) {
        jdbc.update("DELETE FROM sec_comments WHERE id = ?", id);
    }
}
