package com.example.secapp.common.dto;

import com.example.secapp.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * パスワード変更フォーム入力をバインドする DTO。
 */
@Data
public class PasswordChangeForm {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 4, max = 100)
    @MaxUtf8Bytes(72)
    private String newPassword;

    /**
     * デバッグ用。両パスワードはログに載せない。
     *
     * @return マスク済み要約
     */
    @Override
    public String toString() {
        return "PasswordChangeForm(currentPassword=[PROTECTED], newPassword=[PROTECTED])";
    }
}
