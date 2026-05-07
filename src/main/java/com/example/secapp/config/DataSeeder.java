package com.example.secapp.config;

import com.example.secapp.secure.dao.SecureCommentDao;
import com.example.secapp.secure.dao.SecurePostDao;
import com.example.secapp.secure.dao.SecureUserDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Secure 側（{@code sec_*} テーブル）の初期データ投入を行う {@link CommandLineRunner}。
 * <p>
 * Vulnerable 側は SQL マイグレーションで平文パスワードを直に入れるのが教材の前提のため、
 * こちらでは起動時に BCrypt でハッシュ化したうえで INSERT する。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final SecureUserDao userDao;
    private final SecurePostDao postDao;
    private final SecureCommentDao commentDao;
    private final PasswordEncoder passwordEncoder;

    /**
     * アプリ起動後に一度だけ、Secure 側のユーザー・投稿・コメントをシードする。
     * <p>
     * すでに {@code sec_users} に行が存在する場合は何もしない。
     *
     * @param args コマンドライン引数（未使用）
     */
    @Override
    public void run(String... args) {
        if (!userDao.findAll().isEmpty()) {
            log.info("[Seeder] sec_users already populated, skipping.");
            return;
        }
        log.info("[Seeder] populating secure-side initial data with BCrypt-hashed passwords");

        userDao.create("admin", passwordEncoder.encode("admin123"),  "admin@example.com", "ADMIN");
        userDao.create("alice", passwordEncoder.encode("wonderland"),"alice@example.com", "USER");
        userDao.create("bob",   passwordEncoder.encode("builder"),   "bob@example.com",   "USER");

        Long adminId = userDao.findByUsername("admin").orElseThrow().getId();
        Long aliceId = userDao.findByUsername("alice").orElseThrow().getId();
        Long bobId   = userDao.findByUsername("bob").orElseThrow().getId();

        Long welcomeId = postDao.create(adminId, "ようこそ",
                "管理者からのお知らせです。本文は HTML エスケープされて表示されます。");
        postDao.create(aliceId, "初投稿", "こんにちは、Alice です。");
        postDao.create(bobId,   "雑談",   "Bob です。よろしく。");

        commentDao.create(welcomeId, aliceId, "お知らせ確認しました。");
        commentDao.create(welcomeId, bobId,   "よろしくお願いします。");
    }
}
