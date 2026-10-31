package com.example.secapp;

import com.example.secapp.common.entity.Post;
import com.example.secapp.config.SecurityConfig;
import com.example.secapp.docs.web.ErrorPageController;
import com.example.secapp.secure.auth.LoginAttemptService;
import com.example.secapp.secure.auth.SecurePrincipal;
import com.example.secapp.secure.auth.SecureUserDetailsService;
import com.example.secapp.secure.dao.SecureCommentDao;
import com.example.secapp.secure.dao.SecurePostDao;
import com.example.secapp.secure.dao.SecureUserDao;
import com.example.secapp.secure.web.SecureAuthController;
import com.example.secapp.secure.web.SecurePostController;
import com.example.secapp.secure.web.SecureUserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 対策版（{@code /secure/**}）が守るべきセキュリティ上の性質を、Spring Security の設定込みで検証する。
 * <p>
 * 成功時に担保されるのは、CSRF・認可・入力検証・ログイン試行回数の制限が {@link SecurityConfig} と
 * 各コントローラで有効なままであること。リファクタリングで対策が外れたときに気づくための安全網。
 */
@WebMvcTest(controllers = {
        SecurePostController.class, SecureUserController.class, SecureAuthController.class, ErrorPageController.class})
@Import({SecurityConfig.class, LoginAttemptService.class})
class SecureAccessControlTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SecurePostDao postDao;

    @MockitoBean
    private SecureCommentDao commentDao;

    @MockitoBean
    private SecureUserDao userDao;

    @MockitoBean
    private SecureUserDetailsService userDetailsService;

    private static SecurePrincipal principal(long userId, String username, String role) {
        return new SecurePrincipal(userId, username, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private static Post postOwnedBy(long postId, long userId) {
        return Post.builder().id(postId).userId(userId).authorName("owner")
                .title("t").content("c").createdAt(LocalDateTime.now()).build();
    }

    /**
     * 成功時の担保: CSRF トークンのない状態変更リクエストは、ログイン済みでも拒否される。
     */
    @Test
    void postWithoutCsrfToken_isForbidden() throws Exception {
        mvc.perform(post("/secure/posts/1/update")
                        .with(user(principal(2L, "alice", "USER")))
                        .param("title", "t").param("content", "c"))
                .andExpect(status().isForbidden());

        verify(postDao, never()).update(anyLong(), anyString(), anyString());
    }

    /**
     * 成功時の担保: 他人の投稿は、URL の ID を書き換えても更新できない（IDOR 対策）。
     */
    @Test
    void updatingOthersPost_isForbidden() throws Exception {
        when(postDao.findById(1L)).thenReturn(Optional.of(postOwnedBy(1L, 3L)));

        mvc.perform(post("/secure/posts/1/update")
                        .with(user(principal(2L, "alice", "USER"))).with(csrf())
                        .param("title", "hacked").param("content", "hacked"))
                .andExpect(status().isForbidden());

        verify(postDao, never()).update(anyLong(), anyString(), anyString());
    }

    /**
     * 成功時の担保: ユーザー一覧は ADMIN だけが開ける。
     */
    @Test
    void userList_isAdminOnly() throws Exception {
        when(userDao.findAll()).thenReturn(List.of());

        mvc.perform(get("/secure/users").with(user(principal(2L, "alice", "USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/secure/users").with(user(principal(1L, "admin", "ADMIN"))))
                .andExpect(status().isOk());
    }

    /**
     * 成功時の担保: 他人のパスワードは、ADMIN であっても変更できない。
     */
    @Test
    void changingOthersPassword_isForbiddenEvenForAdmin() throws Exception {
        mvc.perform(post("/secure/users/2/change-password")
                        .with(user(principal(1L, "admin", "ADMIN"))).with(csrf())
                        .param("currentPassword", "x").param("newPassword", "newpass"))
                .andExpect(status().isForbidden());

        verify(userDao, never()).updatePassword(anyLong(), anyString());
    }

    /**
     * 成功時の担保: 存在しない投稿へのコメントは、DB へ INSERT せず一覧へ戻す（外部キー違反で 500 にしない）。
     */
    @Test
    void commentOnMissingPost_redirectsWithoutInsert() throws Exception {
        when(postDao.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(post("/secure/posts/999/comments")
                        .with(user(principal(2L, "alice", "USER"))).with(csrf())
                        .param("content", "hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/secure/posts"));

        verify(commentDao, never()).create(anyLong(), anyLong(), anyString());
    }

    /**
     * 成功時の担保: 存在チェックのあとに同名ユーザーが登録された場合も、500 にせず登録画面でエラーを表示する。
     */
    @Test
    void registerRaceOnDuplicateUsername_showsError() throws Exception {
        when(userDao.findByUsername("carol")).thenReturn(Optional.empty());
        doThrow(new DuplicateKeyException("duplicate"))
                .when(userDao).create(eq("carol"), anyString(), anyString(), eq("USER"));

        mvc.perform(post("/secure/register").with(csrf())
                        .param("username", "carol").param("password", "secret1")
                        .param("email", "carol@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("secure/register"))
                .andExpect(model().attribute("errorMessage", "そのユーザー名は既に使われています。"));
    }

    /**
     * 成功時の担保: UTF-8 で 72 バイトを超えるパスワードは、BCrypt に渡す前の入力検証で拒否される。
     * <p>
     * 日本語 25 文字は 75 バイト。文字数（{@code @Size(max = 100)}）の制限だけでは通ってしまう長さ。
     */
    @Test
    void registerWithPasswordOver72Bytes_isRejected() throws Exception {
        mvc.perform(post("/secure/register").with(csrf())
                        .param("username", "dave").param("password", "あ".repeat(25))
                        .param("email", "dave@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("secure/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "password"));

        verify(userDao, never()).create(anyString(), anyString(), anyString(), anyString());
    }

    /**
     * 成功時の担保: 同じユーザー名で続けてログインに失敗するとロックされ、正しいパスワードでもログインできなくなる。
     */
    @Test
    void repeatedLoginFailures_lockTheUsername() throws Exception {
        String hash = passwordEncoder.encode("wonderland");
        // 認証後にパスワードが消去されるため、呼び出しごとに新しいインスタンスを返す
        when(userDetailsService.loadUserByUsername("erin")).thenAnswer(inv ->
                new SecurePrincipal(5L, "erin", hash, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/secure/login").with(csrf())
                            .param("username", "erin").param("password", "wrong"))
                    .andExpect(redirectedUrl("/secure/login?error"));
        }
        mvc.perform(post("/secure/login").with(csrf())
                        .param("username", "erin").param("password", "wonderland"))
                .andExpect(redirectedUrl("/secure/login?locked"));
    }

    /**
     * 成功時の担保: ロックの手前で正しいパスワードを入れれば、ログインでき失敗回数が消える。
     */
    @Test
    void successfulLogin_beforeLock_resetsFailures() throws Exception {
        String hash = passwordEncoder.encode("builder");
        when(userDetailsService.loadUserByUsername(any())).thenAnswer(inv ->
                new SecurePrincipal(6L, "frank", hash, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/secure/login").with(csrf())
                            .param("username", "frank").param("password", "wrong"))
                    .andExpect(redirectedUrl("/secure/login?error"));
        }
        mvc.perform(post("/secure/login").with(csrf())
                        .param("username", "frank").param("password", "builder"))
                .andExpect(redirectedUrl("/secure/posts"));
        mvc.perform(post("/secure/login").with(csrf())
                        .param("username", "frank").param("password", "wrong"))
                .andExpect(redirectedUrl("/secure/login?error"));
    }
}
