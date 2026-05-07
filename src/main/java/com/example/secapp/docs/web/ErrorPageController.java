package com.example.secapp.docs.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 認可・例外用の静的エラーページを返すコントローラ。
 * <p>
 * Spring Security の {@code AccessDeniedHandler}（{@code auditingAccessDeniedHandler()}）からフォワードされる先で、
 * 教材として「対策版では 403 を専用画面に振り分けている」ことを示す。
 */
@Controller
@RequestMapping("/error")
public class ErrorPageController {

    /**
     * 403 (Forbidden) 画面を返す。
     *
     * @return ビュー名 {@code error/403}
     */
    @GetMapping("/403")
    public String forbidden() {
        return "error/403";
    }
}
