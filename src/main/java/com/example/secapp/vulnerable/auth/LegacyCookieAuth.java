package com.example.secapp.vulnerable.auth;

import com.example.secapp.common.entity.User;
import com.example.secapp.vulnerable.dao.VulnerableUserDao;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Vulnerable 版の「自前 Cookie 認証」ヘルパー。
 * <p>
 * 署名のない平文 userId Cookie や {@code HttpOnly=false} を利用し、教材としての弱点を再現する。
 */
@Component
@RequiredArgsConstructor
public class LegacyCookieAuth {

    /** 脆弱版でユーザ ID を保持する Cookie 名。 */
    public static final String COOKIE_NAME = "vuln_uid";

    private final VulnerableUserDao userDao;

    /**
     * ログイン成功時に userId を Cookie に格納する。
     *
     * @param res    HTTP レスポンス
     * @param userId ログインしたユーザの ID
     */
    public void login(HttpServletResponse res, Long userId) {
        Cookie cookie = new Cookie(COOKIE_NAME, String.valueOf(userId));
        cookie.setPath("/vulnerable");
        cookie.setHttpOnly(false);
        cookie.setMaxAge(60 * 60 * 24);
        res.addCookie(cookie);
    }

    /**
     * 認証 Cookie を破棄する。
     *
     * @param res HTTP レスポンス
     */
    public void logout(HttpServletResponse res) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/vulnerable");
        cookie.setMaxAge(0);
        res.addCookie(cookie);
    }

    /**
     * リクエストから Cookie を読み、該当ユーザが存在すれば返す。
     *
     * @param req HTTP リクエスト
     * @return 現在ユーザの {@link Optional}
     */
    public Optional<User> currentUser(HttpServletRequest req) {
        if (req.getCookies() == null) return Optional.empty();
        for (Cookie c : req.getCookies()) {
            if (COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                try {
                    Long uid = Long.parseLong(c.getValue());
                    return userDao.findById(uid);
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}
