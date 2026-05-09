package com.example.secapp.secure.dao;

import com.example.secapp.common.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Secure 版の投稿 DAO。
 * <p>
 * パラメータ化クエリと {@link GeneratedKeyHolder} による安全な INSERT を用いる。
 */
@Repository
@RequiredArgsConstructor
public class SecurePostDao {

    private final JdbcTemplate jdbc;

    private static final String SELECT_BASE =
            "SELECT p.id, p.user_id, p.title, p.content, p.created_at, u.username AS author_name "
                    + "FROM sec_posts p JOIN sec_users u ON u.id = p.user_id ";

    private static final RowMapper<Post> ROW_MAPPER = (rs, n) -> Post.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .authorName(rs.getString("author_name"))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    /**
     * 投稿を新しい順に一覧取得する。
     *
     * @return 投稿リスト
     */
    public List<Post> findAll() {
        return jdbc.query(SELECT_BASE + "ORDER BY p.id DESC", ROW_MAPPER);
    }

    /**
     * タイトル・本文に対する部分一致検索を行う。
     * <p>
     * 値はパラメータ化クエリでバインドするうえ、LIKE 用の <code>%</code>／<code>_</code>／<code>\</code> を
     * バックスラッシュでエスケープし、SQL 側にも {@code ESCAPE '\\'} を明示する。これにより
     * 検索語に <code>%</code> や <code>_</code> を含めても「全件マッチ」「単一文字ワイルドカード」が成立しない。
     *
     * @param keyword 検索語
     * @return ヒットした投稿リスト
     */
    public List<Post> search(String keyword) {
        String like = "%" + escapeLike(keyword) + "%";
        return jdbc.query(
                SELECT_BASE
                        + "WHERE p.title LIKE ? ESCAPE '\\\\' "
                        + "OR p.content LIKE ? ESCAPE '\\\\' "
                        + "ORDER BY p.id DESC",
                ROW_MAPPER, like, like);
    }

    /**
     * LIKE のメタ文字 ({@code %}, {@code _}, {@code \}) をバックスラッシュでエスケープする。
     *
     * @param keyword 入力語（{@code null} 可）
     * @return エスケープ済みの文字列。{@code null} 入力は空文字を返す。
     */
    private static String escapeLike(String keyword) {
        if (keyword == null) return "";
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 主キーで投稿を 1 件取得する。
     *
     * @param id 投稿 ID
     * @return 該当があれば {@link Optional}
     */
    public Optional<Post> findById(Long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    SELECT_BASE + "WHERE p.id = ?", ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 投稿を挿入し、採番された ID を返す。
     *
     * @param userId  投稿者 ID
     * @param title   タイトル
     * @param content 本文
     * @return 新規投稿の ID
     */
    public Long create(Long userId, String title, String content) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sec_posts (user_id, title, content) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).longValue();
    }

    /**
     * 投稿のタイトルと本文を更新する。
     *
     * @param id      投稿 ID
     * @param title   タイトル
     * @param content 本文
     */
    public void update(Long id, String title, String content) {
        jdbc.update("UPDATE sec_posts SET title = ?, content = ? WHERE id = ?",
                title, content, id);
    }

    /**
     * コメントを削除したうえで投稿を削除する。
     * <p>
     * 2 つの {@code UPDATE/DELETE} を <strong>1 トランザクション</strong>に束ねるため
     * {@link Transactional} を付与している。教材として「対策版は両方が成立 or 両方失敗」を担保する。
     *
     * @param id 投稿 ID
     */
    @Transactional
    public void delete(Long id) {
        jdbc.update("DELETE FROM sec_comments WHERE post_id = ?", id);
        jdbc.update("DELETE FROM sec_posts WHERE id = ?", id);
    }
}
