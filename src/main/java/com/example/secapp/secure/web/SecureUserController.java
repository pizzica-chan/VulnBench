package com.example.secapp.secure.web;

import com.example.secapp.common.dto.PasswordChangeForm;
import com.example.secapp.common.entity.User;
import com.example.secapp.secure.auth.SecurePrincipal;
import com.example.secapp.secure.dao.SecureUserDao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Secure 版のユーザ一覧・プロフィール・設定変更コントローラ。
 */
@Controller
@RequestMapping("/secure/users")
@RequiredArgsConstructor
public class SecureUserController {

    private final SecureUserDao userDao;
    private final PasswordEncoder passwordEncoder;

    /**
     * 全ユーザの一覧画面を返す（{@link SecurityConfig} により ADMIN のみ {@code GET} 可能）。
     *
     * @param model ビューモデル
     * @return ビュー {@code secure/users}
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userDao.findAll());
        return "secure/users";
    }

    /**
     * 指定 ID のユーザプロフィールを表示する。
     *
     * @param id    ユーザー ID
     * @param me    ログインユーザ（未ログイン時は {@code null}。本人かどうかの表示に使用）
     * @param model ビューモデル
     * @return 詳細または一覧へ
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal SecurePrincipal me,
                         Model model) {
        User user = userDao.findById(id).orElse(null);
        if (user == null) return "redirect:/secure/posts";
        model.addAttribute("user", user);
        boolean owner = me != null && me.getUserId().equals(user.getId());
        model.addAttribute("isOwner", owner);
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "secure/user_detail";
    }

    /**
     * 本人または管理者のみ、メールアドレスを更新できる。
     *
     * @param id    対象ユーザー ID
     * @param email 新メール
     * @param me    ログインユーザ
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{id}/update-email")
    public String updateEmail(@PathVariable Long id,
                              @RequestParam String email,
                              @AuthenticationPrincipal SecurePrincipal me) {
        ensureOwner(id, me);
        userDao.updateEmail(id, email);
        return "redirect:/secure/users/" + id;
    }

    /**
     * 現パスワードの照合を経て、新パスワードを BCrypt 化して保存する。
     *
     * @param id    対象ユーザー ID
     * @param form  パスワード変更フォーム
     * @param br    検証結果
     * @param me    ログインユーザ
     * @param ra    フラッシュメッセージ
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{id}/change-password")
    public String changePassword(@PathVariable Long id,
                                 @Valid @ModelAttribute PasswordChangeForm form,
                                 BindingResult br,
                                 @AuthenticationPrincipal SecurePrincipal me,
                                 RedirectAttributes ra) {
        ensureOwner(id, me);
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMessage", "入力内容を確認してください。");
            return "redirect:/secure/users/" + id;
        }
        User current = userDao.findById(id).orElseThrow();
        if (!passwordEncoder.matches(form.getCurrentPassword(), current.getPassword())) {
            ra.addFlashAttribute("errorMessage", "現パスワードが正しくありません。");
            return "redirect:/secure/users/" + id;
        }
        userDao.updatePassword(id, passwordEncoder.encode(form.getNewPassword()));
        ra.addFlashAttribute("flashMessage", "パスワードを変更しました。");
        return "redirect:/secure/users/" + id;
    }

    /**
     * 操作対象が本人であるか、管理者であるかを検証する。
     *
     * @param targetId 対象ユーザ ID
     * @param me       ログインユーザ
     * @throws AccessDeniedException 権限不足
     */
    private void ensureOwner(Long targetId, SecurePrincipal me) {
        boolean isAdmin = me.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !me.getUserId().equals(targetId)) {
            throw new AccessDeniedException("not the owner");
        }
    }
}
