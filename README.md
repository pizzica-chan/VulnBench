# SecApp - Spring Boot サイバーセキュリティ学習アプリ

同じ機能を持つ「脆弱版 (`/vulnerable/*`)」と「対策版 (`/secure/*`)」を 1 つの Spring Boot アプリの中に並べて実装し、
**コードと挙動の差分から脆弱性対策の勘所を学ぶ**ための教材アプリです。

## カバーしている脆弱性

- SQL インジェクション
- XSS（蓄積型・反射型）
- CSRF
- パスワード平文保存
- セッション管理不備（自前 Cookie の改ざん / セッション固定化）
- 認可不備 / IDOR

詳しい解説と攻撃ペイロード例は、起動後に `http://localhost:8080/docs` を参照してください。

## 必要環境

- JDK 21
- Maven 3.9+
- Docker / Docker Compose（MySQL 用）

## 起動手順

```bash
# 1. MySQL を起動
docker compose up -d

# 2. アプリを起動
./mvnw spring-boot:run
# もしくは
mvn spring-boot:run
```

ブラウザで `http://localhost:8080` を開くとランディングページが表示されます。

## 初期ユーザー（両版共通の認証情報）

| ユーザー名 | パスワード   | 役割  |
|------------|--------------|-------|
| admin      | admin123     | ADMIN |
| alice      | wonderland   | USER  |
| bob        | builder      | USER  |

VULNERABLE 側はパスワードを平文のまま `vuln_users` に保存しています。
SECURE 側は同じパスワードを BCrypt ハッシュ化して `sec_users` に保存しています（`DataSeeder` が起動時に投入）。

## URL マップ

```
/                          ランディング
/docs                      脆弱性解説 一覧
/docs/{id}                 個別解説 (sqli/xss/csrf/auth/session/idor)

/vulnerable/login          ログイン
/vulnerable/register       登録
/vulnerable/logout         ログアウト
/vulnerable/posts          投稿一覧 / 検索 (?q=)
/vulnerable/posts/new      投稿作成
/vulnerable/posts/{id}     詳細 + コメント
/vulnerable/posts/{id}/edit
/vulnerable/posts/{id}/delete
/vulnerable/users          ユーザー一覧
/vulnerable/users/{id}     プロフィール

/secure/...                上記と同じパス構成
```

## ディレクトリ構成（重要部分）

```
src/main/java/com/example/secapp/
  common/        共通エンティティ・DTO（両版で共有）
  config/        SecurityConfig（/secure/** にだけ Spring Security 適用）, DataSeeder
  vulnerable/    web / service / dao / auth   ←脆弱版の本体
  secure/        web / service / dao / auth   ←対策版の本体（vulnerable と鏡像）
  docs/          ランディング・解説ページ用 Controller
src/main/resources/
  application.yml
  templates/
    fragments/layout.html       共通レイアウトフラグメント
    index.html                  ランディング
    docs/                       解説ページ
    vulnerable/                 脆弱版ビュー (th:utext / CSRF 無し / GET フォーム)
    secure/                     対策版ビュー (th:text / POST + CSRF / オーナー確認)
  db/migration/
    V1__init.sql                vuln_* / sec_* テーブル + vuln 側の初期データ
```

## 学習シナリオ

各シナリオは「VULNERABLE 側で攻撃成立を確認 → SECURE 側で同じ手で失敗するのを確認 → コードを diff で比較」の順がオススメです。

1. **SQLi**: `/vulnerable/login` でユーザー名 `' OR '1'='1' -- ` （末尾スペース）でログイン → admin になりすませる
2. **蓄積型 XSS**: `/vulnerable/posts/new` で本文に `<script>alert(document.cookie)</script>` → 一覧表示で発火
3. **反射型 XSS**: `/vulnerable/posts?q=<img src=x onerror=alert(1)>`
4. **CSRF**: `<img src="http://localhost:8080/vulnerable/posts/1/delete">` を別ページから踏ませる
5. **パスワード平文**: `/vulnerable/users` でパスワードがそのまま見える / `vuln_users` テーブルを直接覗く
6. **セッション改ざん**: ログイン後 DevTools で `vuln_uid` Cookie を `1` に書き換え → admin になりすまし
7. **IDOR**: `bob` でログインし `/vulnerable/users/1/update-email?email=hacked@evil.com`

同じ操作を `/secure/...` 側で試すと、各対策（パラメータ化クエリ・エスケープ・CSRF トークン・BCrypt・セッション再生成・オーナー判定）によって成立しないことが確認できます。

## 重要な注意

- これは学習用アプリです。本番運用には絶対に使わないでください。
- `/vulnerable/**` の挙動は教育目的で**故意に脆弱**にしてあります。
- インターネットに公開しないこと。ローカル環境だけで動かしてください。
