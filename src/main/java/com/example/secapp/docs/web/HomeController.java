package com.example.secapp.docs.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * アプリ直下（{@code /}）のランディングページを返すコントローラ。
 */
@Controller
public class HomeController {

    /**
     * トップページのテンプレート名を返す。
     *
     * @return ビュー名 {@code index}
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
