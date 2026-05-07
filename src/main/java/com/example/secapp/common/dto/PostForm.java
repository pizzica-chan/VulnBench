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

    @NotBlank
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
