package com.example.secapp.docs.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * 脆弱性解説ページ {@code /docs/**} を扱うコントローラ。
 */
@Controller
@RequestMapping("/docs")
public class DocsController {

    /**
     * 一覧で教材カードを表示する順序を固定するためのマスタリスト。
     */
    private static final List<Map<String, String>> VULN_LIST = List.of(
            Map.of("id", "sqli",    "title", "SQL インジェクション",
                    "summary", "ログイン処理に SQL 文字列連結を使うとどうなるか"),
            Map.of("id", "xss",     "title", "クロスサイトスクリプティング (XSS)",
                    "summary", "投稿本文を th:utext で出すと何が起きるか"),
            Map.of("id", "csrf",    "title", "クロスサイトリクエストフォージェリ (CSRF)",
                    "summary", "GET で削除を受け付けてしまう罠"),
            Map.of("id", "auth",    "title", "パスワード平文保存",
                    "summary", "DB に平文を入れたときの被害範囲（脆弱版は ADMIN にだけ一覧リンクだが URL は無認可）"),
            Map.of("id", "session", "title", "セッション管理不備",
                    "summary", "自前 Cookie で userId を持たせるとどうなるか"),
            Map.of("id", "idor",    "title", "認可不備 (IDOR)",
                    "summary", "URL の {id} を書き換えるだけで他人のデータを操作できる")
    );

    /**
     * 脆弱性解説の一覧ページを返す。
     *
     * @param model {@code vulnerabilities} にマスタリストを渡す
     * @return ビュー名 {@code docs/index}
     */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("vulnerabilities", VULN_LIST);
        return "docs/index";
    }

    /**
     * 個別脆弱性ページのビューを返す。存在しない {@code id} の場合は一覧へリダイレクトする。
     *
     * @param id ページ識別子（例 {@code sqli}）
     * @return テンプレート {@code docs/{id}} または {@code redirect:/docs}
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id) {
        boolean exists = VULN_LIST.stream().anyMatch(v -> v.get("id").equals(id));
        if (!exists) return "redirect:/docs";
        return "docs/" + id;
    }
}
