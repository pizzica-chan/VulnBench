package com.example.secapp;

import com.example.secapp.secure.dao.SecureCommentDao;
import com.example.secapp.secure.dao.SecurePostDao;
import com.example.secapp.secure.web.SecurePostController;
import com.example.secapp.vulnerable.auth.LegacyCookieAuth;
import com.example.secapp.vulnerable.dao.VulnerableCommentDao;
import com.example.secapp.vulnerable.dao.VulnerablePostDao;
import com.example.secapp.vulnerable.web.VulnerablePostController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 脆弱版と安全版の実装差分を回帰テストで固定する。
 * <p>
 * 成功時に担保されるのは「本番相当の挙動そのもの」ではなく、
 * 教材として意図した脆弱パターン／修正パターンがコード上で維持されていること。
 * 将来リファクタリングで差分が消えたり逆転したりしないための安全網。
 */
class RegressionLearningContrastTest {

    /**
     * 成功時の担保: 脆弱版 {@code VulnerableCommentDao#update} が
     * ユーザー入力を SQL 文字列へそのまま連結していること。
     * <p>
     * 入力中の {@code '} や {@code --} が SQL 本体に混入するため、
     * SQL インジェクションの教材としての「危険な実装」が維持されている。
     */
    @Test
    void vulnerableCommentUpdate_concatenatesRawInputIntoSql() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        VulnerableCommentDao dao = new VulnerableCommentDao(jdbc);
        String payload = "x' WHERE id = 1 OR 1=1 -- ";

        dao.update(7L, payload);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains(payload);
        assertThat(sql).contains("WHERE id = 7");
    }

    /**
     * 成功時の担保: 安全版 {@code SecureCommentDao#update} が
     * プレースホルダ付きのパラメータ化クエリを使い、入力値は SQL 文に埋め込まれないこと。
     * <p>
     * 同じ攻撃文字列を渡しても JDBC 引数として渡されるだけであり、
     * SQL インジェクション対策としての実装差分が維持されている。
     */
    @Test
    void secureCommentUpdate_usesParameterizedQuery() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SecureCommentDao dao = new SecureCommentDao(jdbc);
        String payload = "x' WHERE id = 1 OR 1=1 -- ";

        dao.update(7L, payload);

        verify(jdbc).update("UPDATE sec_comments SET content = ? WHERE id = ?", payload, 7L);
        verifyNoMoreInteractions(jdbc);
    }

    /**
     * 成功時の担保: 削除操作の HTTP メソッド制約が脆弱版と安全版で意図どおり分かれていること。
     * <ul>
     *   <li>脆弱版: GET {@code /vulnerable/posts/{id}/delete} で削除が実行される（CSRF 等の教材用）</li>
     *   <li>安全版: 同相当の GET 要求は 405 Method Not Allowed となり、副作用のある操作を GET で受け付けない</li>
     * </ul>
     */
    @Test
    void vulnerableDeleteAcceptsGetWhileSecureRejectsGet() throws Exception {
        VulnerablePostDao vulnerablePostDao = mock(VulnerablePostDao.class);
        VulnerableCommentDao vulnerableCommentDao = mock(VulnerableCommentDao.class);
        LegacyCookieAuth legacyCookieAuth = mock(LegacyCookieAuth.class);
        when(legacyCookieAuth.currentUser(any())).thenReturn(java.util.Optional.empty());

        MockMvc vulnerableMvc = MockMvcBuilders
                .standaloneSetup(new VulnerablePostController(vulnerablePostDao, vulnerableCommentDao, legacyCookieAuth))
                .build();

        vulnerableMvc.perform(get("/vulnerable/posts/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vulnerable/posts"));

        SecurePostDao securePostDao = mock(SecurePostDao.class);
        SecureCommentDao secureCommentDao = mock(SecureCommentDao.class);
        MockMvc secureMvc = MockMvcBuilders
                .standaloneSetup(new SecurePostController(securePostDao, secureCommentDao))
                .build();

        secureMvc.perform(get("/secure/posts/1/delete"))
                .andExpect(status().isMethodNotAllowed());
    }
}
