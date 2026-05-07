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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * @param me    ログインユーザ（未ログインなら {@code null}）
     * @param model ビューモデル
     * @return 詳細ビューまたは一覧へ
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(value = "editCommentId", required = false) Long editCommentId,
                         @AuthenticationPrincipal SecurePrincipal me,
                         Model model) {
        Post post = postDao.findById(id).orElse(null);
        if (post == null) return "redirect:/secure/posts";
        List<Comment> comments = commentDao.findByPostId(id);
        Set<Long> manageableCommentIds = new HashSet<>();
        for (Comment c : comments) {
            if (canManageComment(c, me)) manageableCommentIds.add(c.getId());
        }
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("commentForm", new CommentForm());
        model.addAttribute("canManagePost", canManagePost(post, me));
        model.addAttribute("currentUserId", me == null ? null : me.getUserId());
        model.addAttribute("isAdmin", me != null && hasAdmin(me));
        model.addAttribute("manageableCommentIds", manageableCommentIds);
        model.addAttribute("editCommentId", editCommentId);
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
        Post post = postDao.findById(id).orElse(null);
        if (post == null) return "redirect:/secure/posts";
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
        Post post = postDao.findById(id).orElse(null);
        if (post == null) return "redirect:/secure/posts";
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
        Post post = postDao.findById(id).orElse(null);
        if (post == null) return "redirect:/secure/posts";
        ensureOwner(post, me);
        postDao.delete(id);
        return "redirect:/secure/posts";
    }

    /**
     * コメント本文を更新する。コメント主または ADMIN のみ。
     *
     * @param postId    投稿 ID
     * @param commentId コメント ID
     * @param content   新しい本文
     * @param me        ログインユーザ
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{postId}/comments/{commentId}/update")
    public String updateComment(@PathVariable Long postId,
                                @PathVariable Long commentId,
                                @RequestParam String content,
                                @AuthenticationPrincipal SecurePrincipal me) {
        Comment c = commentDao.findById(commentId).orElse(null);
        if (c == null || !c.getPostId().equals(postId)) {
            return "redirect:/secure/posts/" + postId;
        }
        ensureCommentOwner(c, me);
        commentDao.update(commentId, content);
        return "redirect:/secure/posts/" + postId;
    }

    /**
     * コメントを削除する。コメント主または ADMIN のみ。
     *
     * @param postId    投稿 ID
     * @param commentId コメント ID
     * @param me        ログインユーザ
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{postId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long postId,
                                @PathVariable Long commentId,
                                @AuthenticationPrincipal SecurePrincipal me) {
        Comment c = commentDao.findById(commentId).orElse(null);
        if (c == null || !c.getPostId().equals(postId)) {
            return "redirect:/secure/posts/" + postId;
        }
        ensureCommentOwner(c, me);
        commentDao.delete(commentId);
        return "redirect:/secure/posts/" + postId;
    }

    /**
     * 投稿のオーナーであるか、管理者ロールを持つかを検証する。
     *
     * @param post 対象投稿
     * @param me   ログインユーザ
     * @throws AccessDeniedException 権限がない場合
     */
    private void ensureOwner(Post post, SecurePrincipal me) {
        if (me == null || (!hasAdmin(me) && !post.getUserId().equals(me.getUserId()))) {
            throw new AccessDeniedException("not the owner");
        }
    }

    /**
     * コメントのオーナーであるか、管理者ロールを持つかを検証する。
     *
     * @param comment 対象コメント
     * @param me      ログインユーザ
     * @throws AccessDeniedException 権限がない場合
     */
    private void ensureCommentOwner(Comment comment, SecurePrincipal me) {
        if (me == null || (!hasAdmin(me) && !comment.getUserId().equals(me.getUserId()))) {
            throw new AccessDeniedException("not the comment owner");
        }
    }

    /**
     * 画面上の「コメント編集・削除」表示可否を判定する（{@code null} 安全）。
     *
     * @param comment 対象コメント
     * @param me      ログインユーザ
     * @return コメント主または ADMIN なら {@code true}
     */
    private static boolean canManageComment(Comment comment, SecurePrincipal me) {
        if (comment == null || me == null) {
            return false;
        }
        return hasAdmin(me) || comment.getUserId().equals(me.getUserId());
    }

    /**
     * 投稿の編集・削除ボタンを表示してよいかを判定する（{@code null} 安全）。
     *
     * @param post 対象投稿
     * @param me   ログインユーザ（{@code null} 可）
     * @return 投稿者または ADMIN なら {@code true}
     */
    private static boolean canManagePost(Post post, SecurePrincipal me) {
        if (post == null || me == null) {
            return false;
        }
        return hasAdmin(me) || post.getUserId().equals(me.getUserId());
    }

    /**
     * ログインユーザが ADMIN ロールを持つかを判定する。
     *
     * @param me ログインユーザ
     * @return ADMIN なら {@code true}
     */
    private static boolean hasAdmin(SecurePrincipal me) {
        return me.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
