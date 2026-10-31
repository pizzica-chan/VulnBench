package com.example.secapp.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投稿作成・編集フォーム入力をバインドする DTO。
 */
@Data
@NoArgsConstructor
public class PostForm {
    @NotBlank
    @Size(max = 255)
    private String title;

    /** DB の {@code TEXT} 列は 65,535 バイトまで。日本語（1 文字 3 バイト）でも収まる文字数に抑える。 */
    @NotBlank
    @Size(max = 20000)
    private String content;

    /**
     * 本文は長くなりうるため、長さのみを表示する概要文字列を返す。
     *
     * @return 要約
     */
    @Override
    public String toString() {
        return "PostForm(title=" + title + ", content.length=" + (content == null ? 0 : content.length()) + ")";
    }
}
