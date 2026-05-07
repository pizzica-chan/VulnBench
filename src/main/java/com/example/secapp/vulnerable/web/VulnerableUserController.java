package com.example.secapp.vulnerable.web;

import com.example.secapp.common.dto.PasswordChangeForm;
import com.example.secapp.common.entity.User;
import com.example.secapp.vulnerable.auth.LegacyCookieAuth;
import com.example.secapp.vulnerable.dao.VulnerableUserDao;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Vulnerable 版のユーザ一覧・プロフィール・更新コントローラ。
 */
@Controller
@RequestMapping("/vulnerable/users")
@RequiredArgsConstructor
public class VulnerableUserController {

    private final VulnerableUserDao userDao;
    private final LegacyCookieAuth auth;

    /**
     * 全ユーザ一覧を表示する（平文パスワードなどが画面に載る）。
     *
     * @param req   リクエスト
     * @param model ビューモデル
     * @return ビュー {@code vulnerable/users}
     */
    @GetMapping
    public String list(HttpServletRequest req, Model model) {
        model.addAttribute("users", userDao.findAll());
        model.addAttribute("currentUser", auth.currentUser(req).orElse(null));
        return "vulnerable/users";
    }

    /**
     * プロフィール詳細を表示する。認可なしで任意 ID を閲覧可能。
     *
     * @param id    ユーザ ID
     * @param req   リクエスト
     * @param model ビューモデル
     * @return 詳細ビューまたは一覧へ
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest req, Model model) {
        User user = userDao.findById(id).orElse(null);
        if (user == null) return "redirect:/vulnerable/users";
        model.addAttribute("user", user);
        model.addAttribute("currentUser", auth.currentUser(req).orElse(null));
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "vulnerable/user_detail";
    }

    /**
     * GET でメールアドレスを更新する（危険: CSRF / IDOR）。
     *
     * @param id    ユーザ ID
     * @param email 新メール
     * @return 詳細へリダイレクト
     */
    @GetMapping("/{id}/update-email")
    public String updateEmailViaGet(@PathVariable Long id, @RequestParam String email) {
        userDao.updateEmail(id, email);
        return "redirect:/vulnerable/users/" + id;
    }

    /**
     * POST でメールアドレスを更新する（認可なし）。
     *
     * @param id    ユーザ ID
     * @param email 新メール
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{id}/update-email")
    public String updateEmail(@PathVariable Long id, @RequestParam String email) {
        userDao.updateEmail(id, email);
        return "redirect:/vulnerable/users/" + id;
    }

    /**
     * 現パスワード確認なくパスワードを変更する（危険: IDOR）。
     *
     * @param id   ユーザ ID
     * @param form パスワード変更フォーム
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{id}/change-password")
    public String changePassword(@PathVariable Long id, @ModelAttribute PasswordChangeForm form) {
        userDao.updatePassword(id, form.getNewPassword());
        return "redirect:/vulnerable/users/" + id;
    }
}
