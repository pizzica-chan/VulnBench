package com.example.secapp.secure.web;

import com.example.secapp.common.dto.CommentForm;
import com.example.secapp.common.dto.PostForm;
import com.example.secapp.common.entity.Comment;
import com.example.secapp.common.entity.Post;
import com.example.secapp.secure.auth.SecurePrincipal;
import com.example.secapp.secure.dao.SecureCommentDao;
import com.example.secapp.secure.dao.SecurePostDao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import java.util.List;

/**
 * Secure 版の投稿・コメント機能コントローラ。
 */
@Controller
@RequestMapping("/secure/posts")
@RequiredArgsConstructor
public class SecurePostController {

    private final SecurePostDao postDao;
    private final SecureCommentDao commentDao;

    /**
     * 投稿一覧（任意で検索キーワード）を表示する。
     *
     * @param q     検索語（省略可）
     * @param model ビューモデル
     * @return ビュー {@code secure/posts}
     */
    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       Model model) {
        List<Post> posts = (q == null || q.isBlank()) ? postDao.findAll() : postDao.search(q);
        model.addAttribute("posts", posts);
        model.addAttribute("q", q);
        return "secure/posts";
    }

    /**
     * 新規投稿フォームを返す。
     *
     * @param model ビューモデル
     * @return ビュー {@code secure/post_form}
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("postForm", new PostForm());
        return "secure/post_form";
    }

    /**
     * 新規投稿を保存する。
     *
     * @param form 入力フォーム
     * @param br   検証結果
     * @param me   ログインユーザ
     * @param model 検証失敗時の再表示用
     * @return 成功時は詳細へリダイレクト
     */
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute PostForm form,
                         BindingResult br,
                         @AuthenticationPrincipal SecurePrincipal me,
                         Model model) {
        if (br.hasErrors()) return "secure/post_form";
        Long id = postDao.create(me.getUserId(), form.getTitle(), form.getContent());
        return "redirect:/secure/posts/" + id;
    }

    /**
     * 投稿詳細とコメント一覧を表示する。
     *
     * @param id    投稿 ID
     * @param model ビューモデル
     * @return 詳細ビューまたは一覧へ
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Post post = postDao.findById(id).orElse(null);
        if (post == null) return "redirect:/secure/posts";
        List<Comment> comments = commentDao.findByPostId(id);
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("commentForm", new CommentForm());
        return "secure/post_detail";
    }

    /**
     * コメント投稿を処理する。
     *
     * @param id    対象投稿 ID
     * @param form  コメント入力
     * @param br    検証結果
     * @param me    ログインユーザ
     * @param ra    フラッシュメッセージ用
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{id}/comments")
    public String comment(@PathVariable Long id,
                          @Valid @ModelAttribute CommentForm form,
                          BindingResult br,
                          @AuthenticationPrincipal SecurePrincipal me,
                          RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMessage", "コメントを入力してください。");
            return "redirect:/secure/posts/" + id;
        }
        commentDao.create(id, me.getUserId(), form.getContent());
        return "redirect:/secure/posts/" + id;
    }

    /**
     * 投稿編集フォームを表示する。オーナーまたは管理者のみ。
     *
     * @param id    投稿 ID
     * @param me    ログインユーザ
     * @param model ビューモデル
     * @return 編集フォーム
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal SecurePrincipal me,
                           Model model) {
        Post post = postDao.findById(id).orElseThrow();
        ensureOwner(post, me);
        PostForm form = new PostForm();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        model.addAttribute("postForm", form);
        model.addAttribute("post", post);
        return "secure/post_form";
    }

    /**
     * 投稿本文を POST で更新する。
     *
     * @param id    投稿 ID
     * @param form  入力
     * @param br    検証
     * @param me    ログインユーザ
     * @param model エラー時再表示用
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute PostForm form,
                         BindingResult br,
                         @AuthenticationPrincipal SecurePrincipal me,
                         Model model) {
        Post post = postDao.findById(id).orElseThrow();
        ensureOwner(post, me);
        if (br.hasErrors()) {
            model.addAttribute("post", post);
            return "secure/post_form";
        }
        postDao.update(id, form.getTitle(), form.getContent());
        return "redirect:/secure/posts/" + id;
    }

    /**
     * 投稿を削除する。
     *
     * @param id 投稿 ID
     * @param me ログインユーザ
     * @return 一覧へリダイレクト
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal SecurePrincipal me) {
        Post post = postDao.findById(id).orElseThrow();
        ensureOwner(post, me);
        postDao.delete(id);
        return "redirect:/secure/posts";
    }

    /**
     * 投稿のオーナーであるか、管理者ロールを持つかを検証する。
     *
     * @param post 対象投稿
     * @param me   ログインユーザ
     * @throws AccessDeniedException 権限がない場合
     */
    private void ensureOwner(Post post, SecurePrincipal me) {
        boolean isAdmin = me.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !post.getUserId().equals(me.getUserId())) {
            throw new AccessDeniedException("not the owner");
        }
    }
}
