package com.example.secapp.secure.auth;

import com.example.secapp.common.entity.Comment;
import com.example.secapp.common.entity.Post;
import org.springframework.security.access.AccessDeniedException;

/**
 * 対策版の認可ヘルパ。
 * <p>
 * {@code SecurePostController} / {@code SecureUserController} などから使い、
 * 「投稿のオーナーまたは ADMIN」「コメントのオーナーまたは ADMIN」「本人または ADMIN」「本人のみ」を
 * 単一の真実として揃える。{@code null} 安全で、未ログイン時は常に拒否する。
 */
public final class SecureAuthorization {

    /** ADMIN 権限の正準名。 */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private SecureAuthorization() {
    }

    /**
     * ログインユーザが ADMIN ロールを持つかを判定する（{@code null} 可）。
     *
     * @param me ログインユーザ
     * @return ADMIN なら {@code true}
     */
    public static boolean hasAdmin(SecurePrincipal me) {
        if (me == null) {
            return false;
        }
        return me.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
    }

    /**
     * 投稿の編集・削除を許してよいかを {@code null} 安全に判定する。
     *
     * @param post 対象投稿
     * @param me   ログインユーザ
     * @return 投稿者本人または ADMIN なら {@code true}
     */
    public static boolean canManagePost(Post post, SecurePrincipal me) {
        if (post == null || me == null) {
            return false;
        }
        return hasAdmin(me) || me.getUserId().equals(post.getUserId());
    }

    /**
     * コメントの編集・削除を許してよいかを {@code null} 安全に判定する。
     *
     * @param comment 対象コメント
     * @param me      ログインユーザ
     * @return コメント主または ADMIN なら {@code true}
     */
    public static boolean canManageComment(Comment comment, SecurePrincipal me) {
        if (comment == null || me == null) {
            return false;
        }
        return hasAdmin(me) || me.getUserId().equals(comment.getUserId());
    }

    /**
     * 投稿のオーナーまたは ADMIN であることを保証する。違反時は {@link AccessDeniedException}。
     *
     * @param post 対象投稿
     * @param me   ログインユーザ
     */
    public static void ensurePostOwner(Post post, SecurePrincipal me) {
        if (!canManagePost(post, me)) {
            throw new AccessDeniedException("not the post owner");
        }
    }

    /**
     * コメントのオーナーまたは ADMIN であることを保証する。違反時は {@link AccessDeniedException}。
     *
     * @param comment 対象コメント
     * @param me      ログインユーザ
     */
    public static void ensureCommentOwner(Comment comment, SecurePrincipal me) {
        if (!canManageComment(comment, me)) {
            throw new AccessDeniedException("not the comment owner");
        }
    }

    /**
     * 操作対象のユーザー ID が本人または ADMIN であることを保証する。
     *
     * @param targetUserId 対象ユーザー ID
     * @param me           ログインユーザ
     */
    public static void ensureSelfOrAdmin(Long targetUserId, SecurePrincipal me) {
        if (me == null || targetUserId == null
                || (!hasAdmin(me) && !targetUserId.equals(me.getUserId()))) {
            throw new AccessDeniedException("not self or admin");
        }
    }

    /**
     * 操作対象が <strong>本人</strong>であることのみを保証する（ADMIN でも代理不可）。
     *
     * @param targetUserId 対象ユーザー ID
     * @param me           ログインユーザ
     */
    public static void ensureSelf(Long targetUserId, SecurePrincipal me) {
        if (me == null || targetUserId == null || !targetUserId.equals(me.getUserId())) {
            throw new AccessDeniedException("not self");
        }
    }
}
