package com.example.secapp.vulnerable.web;

import com.example.secapp.common.dto.CommentForm;
import com.example.secapp.common.dto.PostForm;
import com.example.secapp.common.entity.Comment;
import com.example.secapp.common.entity.Post;
import com.example.secapp.common.entity.User;
import com.example.secapp.vulnerable.auth.LegacyCookieAuth;
import com.example.secapp.vulnerable.dao.VulnerableCommentDao;
import com.example.secapp.vulnerable.dao.VulnerablePostDao;
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

import java.util.List;
import java.util.Optional;

/**
 * Vulnerable 版の投稿・コメント画面コントローラ。
 */
@Controller
@RequestMapping("/vulnerable/posts")
@RequiredArgsConstructor
public class VulnerablePostController {

    private final VulnerablePostDao postDao;
    private final VulnerableCommentDao commentDao;
    private final LegacyCookieAuth auth;

    /**
     * 投稿一覧（任意で検索）を表示する。
     *
     * @param q     検索語（省略可）
     * @param req   現在ユーザ取得用
     * @param model ビューモデル
     * @return ビュー {@code vulnerable/posts}
     */
    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       HttpServletRequest req,
                       Model model) {
        List<Post> posts = (q == null || q.isBlank()) ? postDao.findAll() : postDao.search(q);
        model.addAttribute("posts", posts);
        model.addAttribute("q", q);
        model.addAttribute("currentUser", auth.currentUser(req).orElse(null));
        return "vulnerable/posts";
    }

    /**
     * 新規投稿フォームを表示する。未ログインならログインへ誘導する。
     *
     * @param req   リクエスト
     * @param model ビューモデル
     * @return ビューまたはリダイレクト
     */
    @GetMapping("/new")
    public String newForm(HttpServletRequest req, Model model) {
        Optional<User> me = auth.currentUser(req);
        if (me.isEmpty()) return "redirect:/vulnerable/login";
        model.addAttribute("postForm", new PostForm());
        model.addAttribute("currentUser", me.get());
        return "vulnerable/post_form";
    }

    /**
     * POST による新規投稿作成。
     *
     * @param form 投稿フォーム
     * @param req  リクエスト（認証チェック）
     * @return 詳細またはログインへ
     */
    @PostMapping("/new")
    public String create(@ModelAttribute PostForm form, HttpServletRequest req) {
        Optional<User> me = auth.currentUser(req);
        if (me.isEmpty()) return "redirect:/vulnerable/login";
        Long id = postDao.create(me.get().getId(), form.getTitle(), form.getContent());
        return "redirect:/vulnerable/posts/" + id;
    }

    /**
     * 投稿詳細とコメント一覧を表示する。
     *
     * @param id    投稿 ID
     * @param req   リクエスト
     * @param model ビューモデル
     * @return 詳細ビューまたは一覧へリダイレクト
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest req, Model model) {
        Post post = postDao.findById(id).orElse(null);
        if (post == null) return "redirect:/vulnerable/posts";
        List<Comment> comments = commentDao.findByPostId(id);
        User me = auth.currentUser(req).orElse(null);
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("commentForm", new CommentForm());
        model.addAttribute("currentUser", me);
        model.addAttribute("canManagePost", canManagePost(me, post));
        return "vulnerable/post_detail";
    }

    /**
     * コメントを追加する。
     *
     * @param id   投稿 ID
     * @param form コメントフォーム
     * @param req  リクエスト
     * @return 詳細ページへリダイレクト
     */
    @PostMapping("/{id}/comments")
    public String comment(@PathVariable Long id,
                          @ModelAttribute CommentForm form,
                          HttpServletRequest req) {
        Optional<User> me = auth.currentUser(req);
        if (me.isEmpty()) return "redirect:/vulnerable/login";
        commentDao.create(id, me.get().getId(), form.getContent());
        return "redirect:/vulnerable/posts/" + id;
    }

    /**
     * 投稿編集フォームを表示する。オーナー判定は行わない（教材用）。
     *
     * @param id    投稿 ID
     * @param req   リクエスト
     * @param model ビューモデル
     * @return 編集フォームまたはリダイレクト
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpServletRequest req, Model model) {
        Post post = postDao.findById(id).orElse(null);
        if (post == null) return "redirect:/vulnerable/posts";
        PostForm form = new PostForm();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        User me = auth.currentUser(req).orElse(null);
        model.addAttribute("postForm", form);
        model.addAttribute("post", post);
        model.addAttribute("currentUser", me);
        model.addAttribute("canManagePost", canManagePost(me, post));
        return "vulnerable/post_form";
    }

    /**
     * GET で投稿を更新する（危険: CSRF 容易／認可なし）。
     *
     * @param id      投稿 ID
     * @param title   タイトル
     * @param content 本文
     * @return 詳細へリダイレクト
     */
    @GetMapping("/{id}/update")
    public String updateViaGet(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam String content) {
        postDao.update(id, title, content);
        return "redirect:/vulnerable/posts/" + id;
    }

    /**
     * POST で投稿を更新する。認可チェックなし。
     *
     * @param id   投稿 ID
     * @param form 投稿フォーム
     * @return 詳細へリダイレクト
     */
    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute PostForm form) {
        postDao.update(id, form.getTitle(), form.getContent());
        return "redirect:/vulnerable/posts/" + id;
    }

    /**
     * GET で投稿を削除する（危険: CSRF / IDOR）。
     *
     * @param id 投稿 ID
     * @return 一覧へリダイレクト
     */
    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        postDao.delete(id);
        return "redirect:/vulnerable/posts";
    }

    /**
     * 画面の「編集・削除」ボタンを出してよいかのみを判定する（サーバ側の認可とは別）。
     * <p>
     * 投稿者本人または ADMIN のとき {@code true}。オーナー検証なしの API と組み合わせて IDOR を学ぶ前提。
     *
     * @param current ログインユーザ（未ログインなら {@code null}）
     * @param post    対象投稿
     * @return ボタン表示に足るなら {@code true}
     */
    private static boolean canManagePost(User current, Post post) {
        if (current == null || post == null) {
            return false;
        }
        if ("ADMIN".equals(current.getRole())) {
            return true;
        }
        return current.getId().equals(post.getUserId());
    }
}
