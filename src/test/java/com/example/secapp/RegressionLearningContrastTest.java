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

class RegressionLearningContrastTest {

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

    @Test
    void secureCommentUpdate_usesParameterizedQuery() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SecureCommentDao dao = new SecureCommentDao(jdbc);
        String payload = "x' WHERE id = 1 OR 1=1 -- ";

        dao.update(7L, payload);

        verify(jdbc).update("UPDATE sec_comments SET content = ? WHERE id = ?", payload, 7L);
        verifyNoMoreInteractions(jdbc);
    }

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
