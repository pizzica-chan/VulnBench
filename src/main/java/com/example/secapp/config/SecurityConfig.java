package com.example.secapp.config;

import com.example.secapp.secure.auth.SecureUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security の設定。
 * <p>
 * {@code /secure/**} 向けの厳格なフィルタチェーンと、それ以外向けの寛容なチェーンを {@code @Order} で分離する。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecureUserDetailsService secureUserDetailsService;

    /**
     * {@code /secure/**} にだけ適用するセキュアな {@link SecurityFilterChain} を構築して返す。
     * <p>
     * フォームログイン、CSRF 有効、セッション ID 再生成、BCrypt による DaoAuthentication を行う。
     * 投稿・ユーザーの一覧／詳細の {@code GET} は教材上「脆弱版と同様に閲覧可能」にし、
     * 新規投稿・編集・{@code POST} 系は認証必須とする。
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
                        .requestMatchers(HttpMethod.GET, "/secure/users").permitAll()
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
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sf -> sf.changeSessionId()))
                .authenticationProvider(daoAuthenticationProvider());

        return http.build();
    }

    /**
     * {@code /secure} 以外のパス向けに、教材用の寛容な {@link SecurityFilterChain} を構築して返す。
     * <p>
     * CSRF を無効化し全リクエストを許可する（脆弱版サイト用）。
     *
     * @param http HTTP セキュリティビルダ
     * @return 構築済みフィルタチェーン
     * @throws Exception 設定処理に失敗した場合
     */
    @Bean
    @Order(2)
    public SecurityFilterChain permissiveFilterChain(HttpSecurity http) throws Exception {
        http
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
