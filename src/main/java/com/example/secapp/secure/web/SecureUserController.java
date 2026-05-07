package com.example.secapp.secure.web;

import com.example.secapp.common.dto.EmailForm;
import com.example.secapp.common.dto.PasswordChangeForm;
import com.example.secapp.common.entity.User;
import com.example.secapp.secure.auth.SecureAuthorization;
import com.example.secapp.secure.auth.SecurePrincipal;
import com.example.secapp.secure.dao.SecureUserDao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Secure 版のユーザ一覧・プロフィール・設定変更コントローラ。
 */
@Controller
@RequestMapping("/secure/users")
@RequiredArgsConstructor
@Slf4j
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
     * <p>
     * ユーザー未存在時は <strong>投稿一覧</strong>（{@code /secure/posts}）へ戻す。
     * 一般ユーザは {@code /secure/users} を開けない（ADMIN のみ）ため、誤って 403 にしないよう
     * 全ロールが見られるトップ系へ遷移させている。
     *
     * @param id    ユーザー ID
     * @param me    ログインユーザ（未ログイン時は {@code null}。本人かどうかの表示に使用）
     * @param model ビューモデル
     * @return 詳細または投稿一覧へ
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal SecurePrincipal me,
                         Model model) {
        User user = userDao.findById(id).orElse(null);
        if (user == null) return "redirect:/secure/posts";
        boolean owner = me != null && me.getUserId().equals(user.getId());
        boolean admin = SecureAuthorization.hasAdmin(me);
        model.addAttribute("user", user);
        model.addAttribute("isOwner", owner);
        model.addAttribute("canEditEmail", owner || admin);
        model.addAttribute("canChangePassword", owner);
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "secure/user_detail";
    }

    /**
     * 本人または管理者のみ、メールアドレスを更新できる。
     * <p>
     * Bean Validation（{@link EmailForm}）で空文字・形式・長さを検査する。検証失敗時は
     * フラッシュメッセージで通知して詳細画面に戻す。
     *
     * @param id    対象ユーザー ID
     * @param form  メール更新フォーム
     * @param br    検証結果
     * @param me    ログインユーザ
     * @param ra    フラッシュメッセージ
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{id}/update-email")
    public String updateEmail(@PathVariable Long id,
                              @Valid @ModelAttribute("emailForm") EmailForm form,
                              BindingResult br,
                              @AuthenticationPrincipal SecurePrincipal me,
                              RedirectAttributes ra) {
        SecureAuthorization.ensureSelfOrAdmin(id, me);
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMessage", "メールアドレスの形式を確認してください。");
            return "redirect:/secure/users/" + id;
        }
        userDao.updateEmail(id, form.getEmail());
        log.info("audit: email updated targetUserId={} byUserId={}", id, me.getUserId());
        ra.addFlashAttribute("flashMessage", "メールアドレスを更新しました。");
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
        SecureAuthorization.ensureSelf(id, me);
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMessage", "入力内容を確認してください。");
            return "redirect:/secure/users/" + id;
        }
        User current = userDao.findById(id).orElseThrow();
        if (!passwordEncoder.matches(form.getCurrentPassword(), current.getPassword())) {
            log.info("audit: password change failed (wrong current password) userId={}", me.getUserId());
            ra.addFlashAttribute("errorMessage", "現パスワードが正しくありません。");
            return "redirect:/secure/users/" + id;
        }
        userDao.updatePassword(id, passwordEncoder.encode(form.getNewPassword()));
        log.info("audit: password changed userId={}", me.getUserId());
        ra.addFlashAttribute("flashMessage", "パスワードを変更しました。");
        return "redirect:/secure/users/" + id;
    }

}
