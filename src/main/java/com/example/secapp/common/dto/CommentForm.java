package com.example.secapp.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * コメント投稿フォーム入力をバインドする DTO。
 * <p>
 * 新規投稿（{@code POST /secure/posts/{id}/comments}）と、コメント更新
 * （{@code POST /secure/posts/{postId}/comments/{commentId}/update}）の両方で再利用される。
 */
@Data
public class CommentForm {
    @NotBlank
    @Size(max = 1000)
    private String content;

    /**
     * ログ用の短い概要を返す。
     *
     * @return content の長さ中心の文字列
     */
    @Override
    public String toString() {
        return "CommentForm(content.length=" + (content == null ? 0 : content.length()) + ")";
    }
}
