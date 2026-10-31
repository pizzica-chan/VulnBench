package com.example.secapp.common.dto;

import jakarta.validation.constraints.Email;
import com.example.secapp.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新規ユーザ登録フォーム入力をバインドする DTO。
 */
@Data
public class RegisterForm {
    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    @NotBlank
    @Size(min = 4, max = 100)
    @MaxUtf8Bytes(72)
    private String password;

    @NotBlank
    @Email
    @Size(max = 128)
    private String email;

    /**
     * バリデーション対象フィールドを伏せずに一覧するデバッグ用文字列。
     *
     * @return 要約（パスワードは長さのみ）
     */
    @Override
    public String toString() {
        return "RegisterForm(username=" + username + ", password.length="
                + (password == null ? 0 : password.length()) + ", email=" + email + ")";
    }
}
