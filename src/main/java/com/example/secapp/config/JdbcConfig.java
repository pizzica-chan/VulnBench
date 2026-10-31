package com.example.secapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * 対策版と脆弱版で、DB に接続するユーザーを分けるための設定。
 * <p>
 * 脆弱版の DAO は SQL インジェクションを再現するため、攻撃者が任意の SQL を実行できる。
 * 同じ DB ユーザーで接続していると、脆弱版の穴から対策版の {@code sec_*} テーブル（BCrypt ハッシュなど）まで読まれる。
 * そこで脆弱版には {@code vuln_*} テーブルにしか権限のない DB ユーザーを使わせる（最小権限の原則）。
 * <p>
 * {@code secapp.vulnerable-datasource.username} が空のときは、従来どおり共通の DB ユーザーで接続する
 * （脆弱版専用ユーザーを用意していない環境でも起動できるようにするため）。
 */
@Configuration
@Slf4j
public class JdbcConfig {

    /**
     * 対策版・Flyway・{@link DataSeeder} が使う、共通の DB ユーザーの {@link JdbcTemplate}。
     *
     * @param dataSource {@code spring.datasource.*} から作られる接続
     * @return 共通の {@link JdbcTemplate}
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 脆弱版の DAO が使う {@link JdbcTemplate}。
     * <p>
     * 接続先 URL は共通の設定を使い、ユーザー名とパスワードだけを差し替える。
     * 教材用で同時接続は少ないため、コネクションプールを持たない {@link DriverManagerDataSource} にしている。
     *
     * @param dataSource 脆弱版専用ユーザーが未設定のときに使う共通の接続
     * @param url        共通の接続先 URL
     * @param username   脆弱版専用の DB ユーザー名（空なら共通の接続を使う）
     * @param password   脆弱版専用の DB パスワード
     * @return 脆弱版用の {@link JdbcTemplate}
     */
    @Bean
    public JdbcTemplate vulnerableJdbcTemplate(DataSource dataSource,
                                               @Value("${spring.datasource.url}") String url,
                                               @Value("${secapp.vulnerable-datasource.username:}") String username,
                                               @Value("${secapp.vulnerable-datasource.password:}") String password) {
        if (username.isBlank()) {
            log.warn("secapp.vulnerable-datasource.username is not set; the vulnerable side shares the main DB user "
                    + "(SQL injection on /vulnerable can read sec_* tables)");
            return new JdbcTemplate(dataSource);
        }
        log.info("vulnerable side connects as DB user '{}'", username);
        return new JdbcTemplate(new DriverManagerDataSource(url, username, password));
    }
}
