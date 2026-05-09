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
- **自動起動スクリプト:** `scripts/` 以下（後述）。**ワンクリック系（`one-click-wsl-*` / `docker-desktop-*`）は Windows 専用**です。**macOS / Linux** ネイティブでは、`scripts/wsl-up.sh` 相当を使うか、リポジトリ直下で `docker compose up --build -d --force-recreate`（停止は `docker compose down`）を実行してください。起動済みスタックを軽く再起動だけする場合は `docker compose restart`、または WSL 用の `./scripts/wsl-restart.sh` / Windows の `one-click-wsl-restart`（後述）。
- **ローカル開発:** JDK 21、Maven 3.9+、Docker（MySQL コンテナのみ）。Docker でビルド・起動する場合はホスト側に JDK/Maven は不要です。
- **ホストポート:** アプリは **`8080`**、MySQL は **`3306`** をホストにバインドします。**既存のローカル MySQL が `3306` で動いている場合は競合**するので、停止するか `docker-compose.yml` の `ports` を `"13306:3306"` のように変更してください。

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

**再起動**（`docker compose restart` 相当。イメージの再ビルドやコンテナの作り直しはしません。スタックがまだ無いと失敗します）:

- **ワンクリック（Windows）:** `scripts\one-click-wsl-restart.cmd` をダブルクリック、または `.\scripts\one-click-wsl-restart.ps1`（`-NoPause` で成功時の Enter 待ちなし）
- **WSL:** リポジトリ直下で `./scripts/wsl-restart.sh`
- **手動:** リポジトリ直下で `docker compose restart`

DB を空に戻して `--force-recreate` したい場合は **`wsl-up.sh` / `one-click-wsl-up`** を使います（上記の再起動では再作成しません）。

| 環境 | 起動 | 停止 | 再起動（起動済み・再ビルドなし） |
|------|------|------|--------------------------------|
| **ワンクリック（Win → WSL）** | `scripts\one-click-wsl-up.cmd` または `.\scripts\one-click-wsl-up.ps1` | `scripts\one-click-wsl-down.cmd` または `.\scripts\one-click-wsl-down.ps1`（ほか上記） | `scripts\one-click-wsl-restart.cmd` または `.\scripts\one-click-wsl-restart.ps1` |
| **WSL（Ubuntu 等）** | `./scripts/wsl-up.sh` | `./scripts/wsl-down.sh` | `./scripts/wsl-restart.sh` |
| **Windows + Docker Desktop（compose は Windows 側 CLI）** | `.\scripts\docker-desktop-up.ps1` または `scripts\docker-desktop-up.cmd` | `.\scripts\docker-desktop-down.ps1` または `scripts\docker-desktop-down.cmd` | リポジトリ直下で `docker compose restart`（専用ワンクリックは無し） |

いずれも **リポジトリのクローン先で実行**してください（カレントディレクトリはどこでも可。スクリプトがルートを自動検出します）。

初回のみ WSL 側で実行権限が付いていない場合:

```bash
chmod +x scripts/wsl-up.sh scripts/wsl-down.sh scripts/wsl-restart.sh
```

#### スクリプト・文字コードの注記（Windows / WSL）

- **`*.sh`（`wsl-up.sh` 等）** … WSL / Linux では **LF 改行**が必要です。CRLF のままだと `bash\r` のようなエラーになります。本リポジトリでは [`.gitattributes`](.gitattributes) で `*.sh text eol=lf` を指定し、Git チェックアウト時に LF になるようにしています。手で編集した場合はエディタの改行を LF にしてください。
- **`scripts/*.ps1`** … **Windows PowerShell 5.1** が、BOM なし UTF-8 の日本語を誤読してパースエラーになることがあるため、**UTF-8（BOM 付き）**で保存しています。編集後にスクリプトが動かなくなったら、保存形式が BOM 付き UTF-8 か確認してください。
- **コンソールの文字化け** … `one-click-wsl-up.cmd` など実行時、コマンドプロンプトの既定コードページのため日本語メッセージが化けることがあります。**処理自体は成功している**ことがあります。見やすくしたい場合は **Windows Terminal** を使う、`chcp 65001` で UTF-8 にする、などで表示を整えられます。

#### Docker での MySQL データ（教材用のリセット）

- **DB はコンテナとともに捨てる前提です。** `docker-compose.yml` では MySQL のデータディレクトリを **tmpfs** に載せており、コンテナの**再作成**や**再起動**で空になります。起動後は Flyway と `DataSeeder` で毎回同じ初期スキーマ・初期データになります。
- **`wsl-up.sh` と `docker-desktop-up.ps1`** は `docker compose up --build -d --force-recreate` を実行するため、これらのスクリプトで起動するたびに **クリーンな DB** になります。
- 手動で `docker compose up -d` だけを使い、**すでに動いている MySQL コンテナを止めずに**再実行した場合は、そのコンテナ内のデータはそのままです（MySQL を止める・作り直すとリセットされます）。
- 古い構成の **名前付きボリューム `secapp-mysql-data`** が環境に残っているなら、**`docker volume rm secapp-mysql-data` で削除推奨**です（現在の compose では定義しておらず、放置するとディスクを食うだけ＋古い学習データが残って混乱の原因になります）。`docker volume ls | findstr secapp` などで確認できます。

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

起動済みのコンテナだけ Spring の再起動などしたいときは `./scripts/wsl-restart.sh`（`docker compose restart`）。WSL 統合を使わず Windows 側だけで compose している場合は同じディレクトリで `docker compose restart`。

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

## URL マップ（主要なもの）

```
/                          ランディング
/docs                      脆弱性解説 一覧
/docs/{id}                 個別解説 (sqli/xss/csrf/auth/session/idor)

/vulnerable/login          ログイン
/vulnerable/register       登録
/vulnerable/logout         ログアウト（GET）
/vulnerable/posts          投稿一覧 / 検索 (?q=)
/vulnerable/posts/new      投稿作成
/vulnerable/posts/{id}     詳細 + コメント（?editCommentId=… でインライン編集）
/vulnerable/posts/{id}/edit                      投稿編集フォーム
/vulnerable/posts/{id}/update                    投稿更新（POST／GET ともに通る教材実装）
/vulnerable/posts/{id}/delete                    投稿削除（GET でも通る）
/vulnerable/posts/{postId}/comments              コメント追加（POST）
/vulnerable/posts/{postId}/comments/{commentId}/update   コメント更新（POST）
/vulnerable/posts/{postId}/comments/{commentId}/delete   コメント削除（GET でも通る）
/vulnerable/users          ユーザー一覧（ADMIN にだけヘッダ2段目に表示／サーバは無認可で URL 直打ち可）
/vulnerable/users/{id}     プロフィール
/vulnerable/users/{id}/update-email              メール更新（GET / POST どちらも通る）
/vulnerable/users/{id}/change-password           パスワード変更（POST、現パス未照合）

/secure/...                上記とほぼ同じパス構成。状態変更系はすべて POST + CSRF。
                            一覧 /secure/users は ADMIN のみ、コメント・投稿削除も POST 限定。
```

## ディレクトリ構成（重要部分）

```
scripts/               WSL / Windows / ワンクリック用の起動・停止スクリプト
  one-click-wsl-up.cmd / one-click-wsl-up.ps1   Windows→WSL ワンクリック起動
  one-click-wsl-down.cmd / one-click-wsl-down.ps1  同上の停止
  one-click-wsl-restart.cmd / one-click-wsl-restart.ps1  同上の再起動（docker compose restart）
  Ensure-DockerDesktop.ps1                      Docker 待機ロジック（他スクリプトから dot-source）
  wsl-up.sh / wsl-down.sh / wsl-restart.sh
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

各シナリオは **「VULNERABLE 側で攻撃成立を確認 → SECURE 側で同じ手で失敗するのを確認 → ソースの `vulnerable/` と `secure/` を diff」** の順がおすすめです。手順の全文は **<http://localhost:8080/docs>** の各ページにあります（前提・初期ユーザ表は解説一覧の先頭に記載）。

| # | テーマ | 脆弱版で行うこと（概要） |
|---|--------|-------------------------|
| 1 | **SQLi（ログイン／検索）** | `/vulnerable/login` でユーザー名に `' OR 1=1 -- `（末尾スペース付き）、パスワード任意でログイン。**攻撃成功の目安:** 投稿一覧の見出し横に「`admin` でログイン中」と出る。検索 SQLi は `/docs/sqli` の手順を参照。 |
| 2 | **蓄積型 XSS** | `alice` / `wonderland` でログイン → 新規投稿の本文に `<script>alert(document.cookie)</script>`。**攻撃成功の目安:** 一覧や詳細でスクリプトが実行されアラートが出る。 |
| 3 | **反射型 XSS** | `/vulnerable/posts` の検索に `<img src=x onerror=alert(1)>` を入力して検索。**攻撃成功の目安:** 一覧表示時に `alert(1)` が動く。 |
| 4 | **CSRF / GET 削除** | ログイン後、新しいタブのアドレスバーに `http://localhost:8080/vulnerable/posts/1/delete` を入力（投稿 ID は一覧で確認）。**攻撃成功の目安:** 確認なしで投稿が消える。 |
| 5 | **パスワード平文** | ヘッダ2段目に一覧が出ない一般ユーザでも、`http://localhost:8080/vulnerable/users` を直打ち。**攻撃成功の目安:** `admin123` など平文が一覧に載る。対策版は一覧が **ADMIN のみ**（ほかはログイン要求または拒否）。 |
| 6 | **セッション改ざん** | `bob` / `builder` でログイン → DevTools の Cookie で `vuln_uid` を `1` に変更 → 一覧を再読み込み。**攻撃成功の目安:** 表示が `admin` に切り替わる。 |
| 7 | **IDOR** | `bob` でログインしたまま `http://localhost:8080/vulnerable/users/1/update-email?email=hacked@evil.com` にアクセス。**攻撃成功の目安:** 一覧 URL を開くか admin プロフィールで、admin のメールが書き換わっている。 |

同じ URL・操作を `/secure/...` に置き換えると、パラメータ化クエリ・エスケープ・CSRF・BCrypt・セッション・オーナー判定などにより成立しません。

## 重要な注意

- これは学習用アプリです。本番運用には絶対に使わないでください。
- `/vulnerable/**` の挙動は教育目的で**故意に脆弱**にしてあります。
- インターネットに公開しないこと。ローカル環境だけで動かしてください。
- `docker-compose.yml` / `application.yml` には MySQL の **学習用クレデンシャル**が平文で書かれています（**アプリ接続: `secapp` / `secapp`**、**MySQL root: `root` / `rootpass`**）。教材限定の前提で、**他環境への流用は避けて**ください。
