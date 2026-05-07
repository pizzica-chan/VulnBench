package com.example.secapp.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * メールアドレス更新フォームをバインドする DTO。
 * <p>
 * 対策版（{@code POST /secure/users/{id}/update-email}）で使用し、
 * Bean Validation により形式・長さを検査する。
 */
@Data
public class EmailForm {
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    /**
     * ログ用の短い概要を返す（メールそのものは出さない）。
     *
     * @return メールアドレス長を含む文字列
     */
    @Override
    public String toString() {
        return "EmailForm(email.length=" + (email == null ? 0 : email.length()) + ")";
    }
}
