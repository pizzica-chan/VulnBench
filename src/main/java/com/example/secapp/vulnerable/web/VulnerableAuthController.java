package com.example.secapp.vulnerable.web;

import com.example.secapp.common.dto.LoginForm;
import com.example.secapp.common.dto.RegisterForm;
import com.example.secapp.common.entity.User;
import com.example.secapp.vulnerable.auth.LegacyCookieAuth;
import com.example.secapp.vulnerable.dao.VulnerableUserDao;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * Vulnerable 版の認証（ログイン・登録・ログアウト）コントローラ。
 */
@Controller
@RequestMapping("/vulnerable")
@RequiredArgsConstructor
public class VulnerableAuthController {

    private final VulnerableUserDao userDao;
    private final LegacyCookieAuth auth;

    /**
     * ログインフォームを表示する。
     *
     * @param model {@code loginForm} を渡す
     * @return ビュー {@code vulnerable/login}
     */
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "vulnerable/login";
    }

    /**
     * ログイン処理を行い、成功時は Cookie でセッション代替を発行する。
     *
     * @param form  フォーム入力
     * @param res   レスポンス（Cookie 設定用）
     * @param model エラー時に再表示するモデル
     * @return 成功時は投稿一覧へリダイレクト、失敗時はログイン画面
     */
    @PostMapping("/login")
    public String doLogin(@ModelAttribute LoginForm form,
                          HttpServletResponse res,
                          Model model) {
        Optional<User> user = userDao.findForLogin(form.getUsername(), form.getPassword());
        if (user.isEmpty()) {
            model.addAttribute("errorMessage", "ユーザー名またはパスワードが違います。");
            model.addAttribute("loginForm", form);
            return "vulnerable/login";
        }
        auth.login(res, user.get().getId());
        return "redirect:/vulnerable/posts";
    }

    /**
     * 新規登録フォームを表示する。
     *
     * @param model {@code registerForm} を渡す
     * @return ビュー {@code vulnerable/register}
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "vulnerable/register";
    }

    /**
     * ユーザ登録を行い、直後に Cookie 認証でログイン状態にする。
     *
     * @param form  登録フォーム
     * @param res   レスポンス
     * @param model 重複名などのエラー用
     * @return 成功時は投稿一覧へ、失敗時は登録画面
     */
    @PostMapping("/register")
    public String doRegister(@ModelAttribute RegisterForm form,
                             HttpServletResponse res,
                             Model model) {
        if (userDao.findByUsername(form.getUsername()).isPresent()) {
            model.addAttribute("errorMessage", "そのユーザー名は既に使われています。");
            model.addAttribute("registerForm", form);
            return "vulnerable/register";
        }
        userDao.create(form.getUsername(), form.getPassword(), form.getEmail());
        Long newId = userDao.findByUsername(form.getUsername()).orElseThrow().getId();
        auth.login(res, newId);
        return "redirect:/vulnerable/posts";
    }

    /**
     * ログアウトし、認証 Cookie を削除する。
     *
     * @param res レスポンス
     * @return ログイン画面へリダイレクト
     */
    @GetMapping("/logout")
    public String logout(HttpServletResponse res) {
        auth.logout(res);
        return "redirect:/vulnerable/login";
    }
}
