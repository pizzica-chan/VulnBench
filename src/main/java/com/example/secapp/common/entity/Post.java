package com.example.secapp.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 掲示板の投稿 1 件を表すドメインオブジェクト。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private Long id;
    private Long userId;
    private String authorName;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    /**
     * 主なフィールドを含むデバッグ用文字列を返す。
     *
     * @return 要約文字列
     */
    @Override
    public String toString() {
        return "Post(id=" + id + ", userId=" + userId + ", authorName=" + authorName + ", title=" + title
                + ", content.length=" + (content == null ? 0 : content.length())
                + ", createdAt=" + createdAt + ")";
    }
}
