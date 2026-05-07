package com.example.secapp.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * コメント投稿フォーム入力をバインドする DTO。
 */
@Data
public class CommentForm {
    @NotBlank
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
