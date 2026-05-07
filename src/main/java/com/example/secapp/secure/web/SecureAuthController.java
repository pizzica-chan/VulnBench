package com.example.secapp.secure.web;

import com.example.secapp.common.dto.LoginForm;
import com.example.secapp.common.dto.RegisterForm;
import com.example.secapp.secure.dao.SecureUserDao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Secure 版のログイン画面表示およびユーザ登録処理を担うコントローラ。
 * <p>
 * 実際の認証は Spring Security のフォームログインフィルタが処理する。
 */
@Controller
@RequestMapping("/secure")
@RequiredArgsConstructor
public class SecureAuthController {

    private final SecureUserDao userDao;
    private final PasswordEncoder passwordEncoder;

    /**
     * ログイン画面を表示する。クエリパラメータに応じメッセージを出し分ける。
     *
     * @param error   認証失敗時に付与されるフラグ
     * @param logout  ログアウト直後に付与されるフラグ
     * @param model   ビューモデル
     * @return ビュー {@code secure/login}
     */
    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        model.addAttribute("loginForm", new LoginForm());
        if (error != null) model.addAttribute("errorMessage", "ユーザー名またはパスワードが違います。");
        if (logout != null) model.addAttribute("flashMessage", "ログアウトしました。");
        return "secure/login";
    }

    /**
     * ユーザ登録フォームを表示する。
     *
     * @param model ビューモデル
     * @return ビュー {@code secure/register}
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "secure/register";
    }

    /**
     * バリデーションを通過した登録内容を BCrypt 化して保存し、ログイン画面へ誘導する。
     *
     * @param form  登録フォーム
     * @param br    バインディング結果
     * @param model ビューモデル
     * @return 検証失敗時は登録画面、成功時はログイン画面
     */
    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute RegisterForm form,
                             BindingResult br,
                             Model model) {
        if (br.hasErrors()) return "secure/register";
        if (userDao.findByUsername(form.getUsername()).isPresent()) {
            model.addAttribute("errorMessage", "そのユーザー名は既に使われています。");
            return "secure/register";
        }
        String hashed = passwordEncoder.encode(form.getPassword());
        userDao.create(form.getUsername(), hashed, form.getEmail(), "USER");
        model.addAttribute("flashMessage", "登録しました。ログインしてください。");
        model.addAttribute("loginForm", new LoginForm());
        return "secure/login";
    }
}
