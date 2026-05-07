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

- **おすすめ:** [Docker Desktop](https://www.docker.com/products/docker-desktop/)（Windows では WSL2 バックエンド推奨）
- **自動起動スクリプト:** `scripts/` 以下（後述）
- **ローカル開発:** JDK 21、Maven 3.9+、Docker（MySQL コンテナのみ）

## 起動手順（自動化）

### ワンクリック（Windows から・WSL 未起動でも可）

**Docker Desktop も WSL も止まっている状態**から、`http://localhost:8080` まで繋ぐ最短ルートです。

1. [Docker Desktop](https://www.docker.com/products/docker-desktop/) をインストールする。
2. Docker Desktop の **Settings → Resources → WSL integration** で、使う Linux ディストリビューションを **オン** にする。
3. 次のいずれかを実行する。
   - **エクスプローラー:** `scripts\one-click-wsl-up.cmd` をダブルクリック
   - **PowerShell:** リポジトリ直下で `.\scripts\one-click-wsl-up.ps1`

流れ: Windows 側で Docker デーモンが応答するまで待つ（必要なら Docker Desktop を起動）→ `wsl.exe` が既定のディストリを立ち上げる → WSL 上で `./scripts/wsl-up.sh`（`docker compose up --build -d`）を実行。

起動後、ブラウザで **`http://localhost:8080`** を開く。コンソールが閉じる前に Enter を求められたら、メッセージを確認してから Enter で終了して構いません。

オプション（PowerShell）:

- **起動 (`one-click-wsl-up.ps1`):** `-FollowLogs` … 起動後に `app` コンテナのログを追従（Ctrl+C で打ち切り。コンテナは動いたまま）／ `-SkipDockerDesktopStart` … Docker Desktop の自動起動は試さない
- **起動・停止どちらも:** `-NoPause` … 成功時の Enter 待ちをしない

停止は次のいずれか（**Docker Desktop が動いてデーモンが応答する状態**が必要です）。

- **ワンクリック（Windows）:** `scripts\one-click-wsl-down.cmd` をダブルクリック、または `.\scripts\one-click-wsl-down.ps1`
- **WSL:** リポジトリ直下で `./scripts/wsl-down.sh`
- **手動:** リポジトリ直下で `docker compose down`（同じプロジェクト名なら Windows / WSL どちらからでも可）

| 環境 | 起動 | 停止 |
|------|------|------|
| **ワンクリック（Win → WSL）** | `scripts\one-click-wsl-up.cmd` または `.\scripts\one-click-wsl-up.ps1` | `scripts\one-click-wsl-down.cmd` または `.\scripts\one-click-wsl-down.ps1`（ほか上記） |
| **WSL（Ubuntu 等）** | `./scripts/wsl-up.sh` | `./scripts/wsl-down.sh` |
| **Windows + Docker Desktop（compose は Windows 側 CLI）** | `.\scripts\docker-desktop-up.ps1` または `scripts\docker-desktop-up.cmd` | `.\scripts\docker-desktop-down.ps1` または `scripts\docker-desktop-down.cmd` |

いずれも **リポジトリのクローン先で実行**してください（カレントディレクトリはどこでも可。スクリプトがルートを自動検出します）。

初回のみ WSL 側で実行権限が付いていない場合:

```bash
chmod +x scripts/wsl-up.sh scripts/wsl-down.sh
```

#### スクリプト・文字コードの注記（Windows / WSL）

- **`*.sh`（`wsl-up.sh` 等）** … WSL / Linux では **LF 改行**が必要です。CRLF のままだと `bash\r` のようなエラーになります。本リポジトリでは [`.gitattributes`](.gitattributes) で `*.sh text eol=lf` を指定し、Git チェックアウト時に LF になるようにしています。手で編集した場合はエディタの改行を LF にしてください。
- **`scripts/*.ps1`** … **Windows PowerShell 5.1** が、BOM なし UTF-8 の日本語を誤読してパースエラーになることがあるため、**UTF-8（BOM 付き）**で保存しています。編集後にスクリプトが動かなくなったら、保存形式が BOM 付き UTF-8 か確認してください。
- **コンソールの文字化け** … `one-click-wsl-up.cmd` など実行時、コマンドプロンプトの既定コードページのため日本語メッセージが化けることがあります。**処理自体は成功している**ことがあります。見やすくしたい場合は **Windows Terminal** を使う、`chcp 65001` で UTF-8 にする、などで表示を整えられます。

### WSL で起動する

前提:

1. Windows に **Docker Desktop** を入れ、**Settings → Resources → WSL integration** で使用中のディストリビューションをオンにする。
2. WSL 内で `docker compose version` が通ること（Docker CLI は Windows の Docker Desktop が提供）。

リポジトリが Windows ドライブ上にある場合の例:

```bash
cd /mnt/d/workspace/secure   # 環境に合わせて変更
./scripts/wsl-up.sh
```

ブラウザは Windows 側で **`http://localhost:8080`** を開けばよい（ポートはホストにバインドされているため）。

ログ確認:

```bash
cd /mnt/d/workspace/secure
docker compose logs -f app
```

停止は `./scripts/wsl-down.sh` または `docker compose down`（リポジトリ直下で）。

### Windows（Docker Desktop）で起動する

**PowerShell**（推奨）:

```powershell
cd D:\workspace\secure   # 環境に合わせて変更
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned   # 初回のみ、必要なら
.\scripts\docker-desktop-up.ps1
```

Docker が止まっている場合、スクリプトが **Docker Desktop の起動を試み、デーモンが応答するまで待機**します（最大約 3 分）。

起動直後からアプリログだけ追いたい場合:

```powershell
.\scripts\docker-desktop-up.ps1 -FollowLogs
```

既に Docker Desktop を手動起動済みで、EXE の起動を試みたくない場合:

```powershell
.\scripts\docker-desktop-up.ps1 -SkipDockerDesktopStart
```

**コマンドプロンプト / エクスプローラーから**は `.cmd` でも可:

```bat
scripts\docker-desktop-up.cmd
scripts\docker-desktop-down.cmd
```

ブラウザで **`http://localhost:8080`** を開いてください。

停止は `.\scripts\docker-desktop-down.ps1` またはリポジトリ直下で `docker compose down`。

### 手動（docker compose のみ）

**Docker Desktop を先に起動**してから、リポジトリ直下で実行します。

```bash
docker compose up --build -d
```

初回はビルドに数分かかることがあります。状態・ログ・停止:

```bash
docker compose ps
docker compose logs -f app
docker compose down
```

### MySQL のみ Docker、アプリはホストで動かす

```bash
docker compose up -d mysql
mvn spring-boot:run
```

JDK 21 と Maven が必要です。`application.yml` は `localhost:3306` を参照します。

ブラウザで `http://localhost:8080` を開いてください。

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
scripts/               WSL / Windows / ワンクリック用の起動・停止スクリプト
  one-click-wsl-up.cmd / one-click-wsl-up.ps1   Windows→WSL ワンクリック起動
  one-click-wsl-down.cmd / one-click-wsl-down.ps1  同上の停止
  Ensure-DockerDesktop.ps1                      Docker 待機ロジック（他スクリプトから dot-source）
  wsl-up.sh / wsl-down.sh
  docker-desktop-up.ps1 / docker-desktop-down.ps1
  docker-desktop-up.cmd / docker-desktop-down.cmd
Dockerfile
docker-compose.yml
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
