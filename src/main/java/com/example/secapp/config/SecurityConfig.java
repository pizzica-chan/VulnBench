package com.example.secapp.config;

import com.example.secapp.secure.auth.SecurePrincipal;
import com.example.secapp.secure.auth.SecureUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Spring Security の設定。
 * <p>
 * {@code /secure/**} 向けの厳格なフィルタチェーンと、それ以外向けの寛容なチェーンを {@code @Order} で分離する。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final SecureUserDetailsService secureUserDetailsService;

    /**
     * {@code /secure/**} にだけ適用するセキュアな {@link SecurityFilterChain} を構築して返す。
     * <p>
     * フォームログイン、CSRF 有効、セッション ID 再生成、BCrypt による DaoAuthentication を行う。
     * ユーザープロフィール {@code GET /secure/users/{id}} は教材上閲覧可能にする一方、
     * <strong>ユーザー一覧 {@code GET /secure/users} は ADMIN のみ</strong>とする。
     * 投稿一覧／詳細の {@code GET} は匿名可。新規投稿・編集・{@code POST} 系は認証必須とする。
     * <p>
     * さらに学習用に <strong>セキュリティヘッダ</strong>を明示的に付与する：
     * <ul>
     *     <li>{@code X-Content-Type-Options: nosniff} — MIME スニッフィング無効化</li>
     *     <li>{@code X-Frame-Options: SAMEORIGIN} — クリックジャッキング防止</li>
     *     <li>{@code X-XSS-Protection: 1; mode=block} — 旧ブラウザ互換</li>
     *     <li>{@code Referrer-Policy: same-origin}</li>
     *     <li>{@code Content-Security-Policy} — インライン script を禁止</li>
     * </ul>
     * 同じヘッダは寛容なチェーン（{@code /vulnerable/**}）には付かないため、レスポンスの差を比較できる。
     *
     * @param http HTTP セキュリティビルダ
     * @return 構築済みフィルタチェーン
     * @throws Exception 設定処理に失敗した場合
     */
    @Bean
    @Order(1)
    public SecurityFilterChain secureFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/secure/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/secure/login", "/secure/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/secure/posts/new").authenticated()
                        .requestMatchers(HttpMethod.GET, "/secure/posts/*/edit").authenticated()
                        .requestMatchers(HttpMethod.GET, "/secure/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/secure/posts/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/secure/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/secure/users/*").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/secure/login")
                        .loginProcessingUrl("/secure/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/secure/posts", false)
                        .failureUrl("/secure/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/secure/logout")
                        .logoutSuccessUrl("/secure/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("SECAPP_SESSION"))
                .csrf(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(f -> f.sameOrigin())
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'self'")))
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sf -> sf.changeSessionId()))
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(auditingAccessDeniedHandler()))
                .authenticationProvider(daoAuthenticationProvider());

        return http.build();
    }

    /**
     * {@code /secure} 以外のパス向けに、教材用の寛容な {@link SecurityFilterChain} を構築して返す。
     * <p>
     * CSRF を無効化し全リクエストを許可する（脆弱版サイト・トップ・{@code /docs/**} など用）。
     * {@code @Order(1)} の {@link #secureFilterChain} が {@code /secure/**} を先取りするため、
     * このチェーンには実際にはそれ以外のパスだけが到達する。{@code securityMatcher("/**")}
     * で「全 URL を対象とする寛容チェーン」であることをコード上も明示する。
     *
     * @param http HTTP セキュリティビルダ
     * @return 構築済みフィルタチェーン
     * @throws Exception 設定処理に失敗した場合
     */
    @Bean
    @Order(2)
    public SecurityFilterChain permissiveFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        return http.build();
    }

    /**
     * アプリケーション全体で使うパスワードエンコーダ（BCrypt）を提供する。
     *
     * @return {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 認可失敗を {@code INFO} ログに残してから既定の 403 ページへフォワードする
     * {@link AccessDeniedHandler} を返す。
     * <p>
     * 教材として「対策版では認可違反が起きたことが監査ログに残る」点を可視化する。
     *
     * @return ログ機能付きの {@link AccessDeniedHandler}
     */
    @Bean
    public AccessDeniedHandler auditingAccessDeniedHandler() {
        AccessDeniedHandlerImpl delegate = new AccessDeniedHandlerImpl();
        delegate.setErrorPage("/error/403");
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            String username = null;
            if (auth != null && auth.getPrincipal() instanceof SecurePrincipal p) {
                userId = p.getUserId();
                username = p.getUsername();
            }
            log.info("audit: access denied method={} uri={} userId={} username={} reason={}",
                    request.getMethod(), request.getRequestURI(), userId, username,
                    accessDeniedException.getMessage());
            delegate.handle(request, response, accessDeniedException);
        };
    }

    /**
     * {@link SecureUserDetailsService} と {@link PasswordEncoder} を束ねた認証プロバイダを提供する。
     *
     * @return 構成済み {@link DaoAuthenticationProvider}
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(secureUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
