package com.example.secapp.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 投稿へのコメント 1 件を表すドメインオブジェクト。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private Long id;
    private Long postId;
    private Long userId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;

    /**
     * デバッグ用の概要文字列を返す。
     *
     * @return 要約文字列
     */
    @Override
    public String toString() {
        return "Comment(id=" + id + ", postId=" + postId + ", userId=" + userId + ", authorName=" + authorName
                + ", content.length=" + (content == null ? 0 : content.length())
                + ", createdAt=" + createdAt + ")";
    }
}
