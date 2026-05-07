package com.example.secapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SecApp アプリケーションのエントリポイント。
 * <p>
 * 脆弱版（{@code /vulnerable/**}）と対策版（{@code /secure/**}）を同一プロセスで提供する学習用 Spring Boot アプリ。
 */
@SpringBootApplication
public class SecAppApplication {

    /**
     * アプリケーションを起動する。
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        SpringApplication.run(SecAppApplication.class, args);
    }
}
