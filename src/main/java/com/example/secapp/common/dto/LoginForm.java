package com.example.secapp.common.dto;

import lombok.Data;

/**
 * Vulnerable / Secure のログイン入力をバインドするフォームDTO。
 */
@Data
public class LoginForm {
    private String username;
    private String password;

    /**
     * デバッグ用。パスワード値はログに載せないように伏せる。
     *
     * @return マスク済み文字列
     */
    @Override
    public String toString() {
        return "LoginForm(username=" + username + ", password=[PROTECTED])";
    }
}
