# SecApp - Spring Boot サイバーセキュリティ学習アプリ

同じ機能を持つ「脆弱版 (`/vulnerable/*`)」と「対策版 (`/secure/*`)」を 1 つの Spring Boot アプリの中に並べて実装し、
**コードと挙動の差分から脆弱性対策の勘所を学ぶ**ための教材アプリです。

## カバーしている脆弱性

- SQL インジェクション
- XSS（蓄積型・反射型）
- CSRF
- パスワード平文保存
- セッション管理不備（脆弱版: 自前 Cookie 改ざん、対策版: セッション ID 再生成など）
- 認可不備 / IDOR

詳しい解説と攻撃ペイロード例は、起動後に `http://localhost:8080/docs` を参照してください。

## 必要環境

- **おすすめ:** [Docker Desktop](https://www.docker.com/products/docker-desktop/)（Windows では WSL2 バックエンド推奨）
- **自動起動スクリプト:** `scripts/` 以下（後述）。**ワンクリック系（`one-click-wsl-*` / `docker-desktop-*`）は Windows 専用**です。**macOS / Linux** ネイティブでは、`scripts/wsl-up.sh` 相当を使うか、リポジトリ直下で `docker compose up --build -d --force-recreate`（停止は `docker compose down`）を実行してください。**ソース変更を Docker へ反映する**ときは `./scripts/wsl-restart.sh` / Windows の `one-click-wsl-restart`（`docker compose up -d --build --force-recreate app`）を使う。コンテナだけ止めて同じイメージで立ち上げ直すだけなら `docker compose restart`。
- **ローカル開発:** JDK 21、Maven 3.9+、Docker（MySQL コンテナのみ）。Docker でビルド・起動する場合はホスト側に JDK/Maven は不要です。
- **AWS CodeBuild:** クラウドでビルドする場合は [AWS（CodeBuild）](#awscodebuild) を参照（単体 CodeBuild 用。ローカルの Docker 起動とは別手順）。
- **AWS デモ公開:** インターネットから ECS タスクのパブリック IP 経由で試す場合は [AWS（デモ公開 / CodePipeline）](#awsデモ公開--codepipeline) を参照（CloudFormation + ワンクリック bat。ALB なし。詳細は [`aws/README.md`](aws/README.md)）。
- **ホストポート:** アプリは **`8080`**、MySQL は **`3306`** をホストにバインドします。**既存のローカル MySQL が `3306` で動いている場合は競合**するので、停止するか `docker-compose.yml` の `ports` を `"13306:3306"` のように変更してください。**`8080` を他プロセスが使用中のときも競合**するため、必要なら `app.ports` を `"18080:8080"` のように変更し、ブラウザ側も `http://localhost:18080` に切り替えてください。

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

**ソース反映（app イメージ再ビルド + app コンテナ載せ替え）**（`docker compose up -d --build --force-recreate app` 相当。**MySQL コンテナは載せ替えません**。MySQL が未起動なら `depends_on` で立ち上がります）。

- **ワンクリック（Windows）:** `scripts\one-click-wsl-restart.cmd` をダブルクリック、または `.\scripts\one-click-wsl-restart.ps1`（`-NoPause` で成功時の Enter 待ちなし）
- **WSL:** リポジトリ直下で `./scripts/wsl-restart.sh`
- **手動:** リポジトリ直下で `docker compose up -d --build --force-recreate app`

**スタック全体を作り直し**て DB を初期状態にしたい場合は **`wsl-up.sh` / `one-click-wsl-up`**（`--force-recreate` 全サービス）を使います。コンテナのみの軽い再起動だけなら `docker compose restart`。

| 環境 | 起動 | 停止 | ソース反映（app 再ビルド） |
|------|------|------|---------------------------|
| **ワンクリック（Win → WSL）** | `scripts\one-click-wsl-up.cmd` または `.\scripts\one-click-wsl-up.ps1` | `scripts\one-click-wsl-down.cmd` または `.\scripts\one-click-wsl-down.ps1`（ほか上記） | `scripts\one-click-wsl-restart.cmd` または `.\scripts\one-click-wsl-restart.ps1` |
| **WSL（Ubuntu 等）** | `./scripts/wsl-up.sh` | `./scripts/wsl-down.sh` | `./scripts/wsl-restart.sh` |
| **Windows + Docker Desktop（compose は Windows 側 CLI）** | `.\scripts\docker-desktop-up.ps1` または `scripts\docker-desktop-up.cmd` | `.\scripts\docker-desktop-down.ps1` または `scripts\docker-desktop-down.cmd` | リポジトリ直下で `docker compose up -d --build --force-recreate app`（専用ワンクリックは無し） |

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

- **DB はコンテナとともに捨てる前提です。** `docker-compose.yml` では MySQL のデータディレクトリを **tmpfs** に載せており、コンテナの**再作成**や**再起動**で空になります。起動後は Flyway と `DataSeeder` で初期スキーマ・初期データを投入します。
- **`wsl-up.sh` と `docker-desktop-up.ps1`** は `docker compose up --build -d --force-recreate` を実行するため、これらのスクリプトで起動するたびに **クリーンな DB** になります。
- 手動で `docker compose up -d` だけを使い、**すでに動いている MySQL コンテナを止めずに**再実行した場合は、そのコンテナ内のデータはそのままです（MySQL を止める・作り直すとリセットされます）。
- `DataSeeder` は **`sec_users` が空のときだけ**投入を実行します。DB を残したままアプリのみ再起動した場合、2回目以降は投入がスキップされます。
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

ソースを直したあと Docker 上のアプリに反映したいときは `./scripts/wsl-restart.sh`（`docker compose up -d --build --force-recreate app`）。WSL 統合を使わず Windows 側だけで compose している場合は同じディレクトリで同じコマンドを実行する。コンテナだけの再起動だけなら `docker compose restart`。

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
Docker でアプリも起動する場合は `SPRING_PROFILES_ACTIVE=docker`（`docker-compose.yml`）により、`application-docker.yml` の `mysql:3306` を参照します。

ブラウザで `http://localhost:8080` を開いてください。

## AWS（デモ公開 / CodePipeline）

**個人デモ向け**に、CloudFormation で ECS Fargate + CodePipeline を一括作成し、タスクの **パブリック IP** から HTTP でアクセスできる構成です（**ALB なし**でコスト抑制）。

| 項目 | 内容 |
|------|------|
| 公開 URL | `http://{タスクのパブリック IP}:8080`（`09_aws-app-url.bat` で表示。再起動で IP は変わる） |
| 配備 | CodePipeline（Build → Test → ECS ローリング） |
| 初期タスク数 | `0`（`04_aws-ecs-start.bat` で手動起動） |
| ビルド定義 | **`aws/buildspec-build.yml`** / **`aws/buildspec-test.yml`** |

ルートの [`buildspec.yml`](buildspec.yml) は **この Pipeline では使いません**（別の CodeBuild プロジェクト用に残しています）。

### ワンクリック bat（リポジトリ直下）

| bat | 内容 |
|-----|------|
| `02_aws-deploy.bat` | CloudFormation デプロイ（初回・更新） |
| `03_aws-pipeline-run.bat` | Pipeline 手動実行 |
| `04_aws-ecs-start.bat` | ECS 起動（desired-count=1） |
| `05_aws-ecs-stop.bat` | ECS 停止（Fargate 課金抑制） |
| `06_aws-stack-delete.bat` | スタック完全削除 |
| `07_aws-mysql-shell.bat` | ECS 上の MySQL クライアント |
| `08_aws-mysql-portforward.bat` | MySQL を localhost:13306 に転送 |
| `09_aws-app-url.bat` | タスクのパブリック IP / URL 表示 |

**注意:** 他プロジェクト（例: gourmet-map）の bat と名前が似ています。**必ず本リポジトリ（VulnBench / secure）直下の bat を実行**してください。

### 初回セットアップ（要点）

1. **GitHub 連携** … AWS コンソールの [Connections](https://console.aws.amazon.com/codesuite/settings/connections) で GitHub を接続し、Connection ARN を取得
2. **`aws/deploy.env` を作成** … `copy aws\deploy.env.example aws\deploy.env` のあと `GITHUB_CONNECTION_ARN` と `REPOSITORY_ID` を設定
3. **`aws/` を GitHub に push** … Pipeline は GitHub からソースを取得するため、**CloudFormation デプロイ前に** `aws/buildspec-build.yml` 等がリモートに存在している必要があります（未 push だと Build が `buildspec-build.yml: no such file` で失敗します）
4. **`02_aws-deploy.bat`** … スタック作成
5. **`03_aws-pipeline-run.bat`** … イメージビルド・ECR push・ECS デプロイ（Git push では自動起動しません）
6. **`04_aws-ecs-start.bat`** … タスク起動
7. **`09_aws-app-url.bat`** … URL 確認

手順の全文・課金・トラブルシュートは **[`aws/README.md`](aws/README.md)** を参照してください。

教材ページ（`/docs`）や [学習シナリオ](#学習シナリオ) の URL は `localhost:8080` 表記です。**AWS デモ**ではホスト部分を `09_aws-app-url.bat` で表示される **`{パブリック IP}:8080`** に読み替えてください。

### ソース変更を AWS に反映するとき

1. 変更を GitHub の `master`（または `deploy.env` の `BRANCH_NAME`）へ push
2. `03_aws-pipeline-run.bat` を実行
3. Deploy 成功後、必要なら `04_aws-ecs-start.bat`（停止中だった場合）

### 注意（教材アプリ × インターネット公開）

本アプリは**意図的に脆弱な教材**です。AWS デモ構成は **HTTP のみ・WAF なし** で URL を知っていれば誰でもアクセスできます。**デモ・検証目的に限定**し、使わないときは `05_aws-ecs-stop.bat` または `06_aws-stack-delete.bat` で課金を抑えてください。

## AWS（CodeBuild）

[`buildspec.yml`](buildspec.yml) は **単体の AWS CodeBuild プロジェクト** 用のビルド手順書です。GitHub などからソースを取得し、クラウド上で **テスト → JAR 作成 → Docker イメージ作成 → ECR へアップロード** までを自動化します。

[AWS（デモ公開 / CodePipeline）](#awsデモ公開--codepipeline) とは **別系統** です。こちらは CodePipeline / ECS デプロイまでは含みません（`imagedefinitions.json` を出力する入口だけ用意しています）。

### 全体の流れ（イメージ）

```
[事前準備] ECR リポジトリ作成・CodeBuild 設定
      ↓
CodeBuild が buildspec.yml を実行
      ↓
  install   … JDK 21 (Corretto) を有効化
  pre_build … ECR にログイン、イメージのタグを決める
  build     … mvn test package → docker build
  post_build… docker push → imagedefinitions.json 出力
      ↓
成果物: secapp.jar / imagedefinitions.json（＋ ECR 上の Docker イメージ）
```

CodeBuild では `commands` の各ブロックが **別プロセス** で動くため、`buildspec.yml` ではフェーズ間で `IMAGE_TAG` などを `.codebuild-env` 経由で引き継いでいます。

### 初回だけやること（チェックリスト）

ビルドを走らせる**前**に、次を揃えてください。

| # | やること | メモ |
|---|----------|------|
| 1 | **ECR リポジトリを作成** | 名前は既定で `secapp`（`buildspec.yml` の `IMAGE_REPO_NAME` と一致させる） |
| 2 | **CodeBuild プロジェクトを作成** | ソースに本リポジトリ、ビルド仕様はリポジトリの `buildspec.yml` |
| 3 | **特権モードを有効** | Docker ビルドに必要（下記「JAR のみ」なら不要） |
| 4 | **リージョンを設定** | 環境変数 `AWS_DEFAULT_REGION` または `AWS_REGION`（例: `ap-northeast-1`） |
| 5 | **サービスロールに IAM を付与** | 下記「IAM」のとおり |

**ECR リポジトリ作成例**（リージョン・リポジトリ名は環境に合わせて変更）:

```bash
aws ecr create-repository --repository-name secapp --region ap-northeast-1
```

リポジトリ名を変えた場合は、CodeBuild の環境変数 `IMAGE_REPO_NAME` も同じ名前にします。

### CodeBuild プロジェクトの設定

| 項目 | 推奨値 |
|------|--------|
| ビルド仕様 | リポジトリ内 `buildspec.yml`（ルートに配置済み） |
| 環境イメージ | `aws/codebuild/amazonlinux-x86_64-standard:5.0` など |
| 特権モード | **有効**（Docker を使う場合） |
| 環境変数 | `AWS_DEFAULT_REGION` = 利用リージョン |

**任意の環境変数**

| 変数 | 既定値 | 説明 |
|------|--------|------|
| `IMAGE_REPO_NAME` | `secapp` | ECR リポジトリ名（#1 で作った名前と一致） |
| `IMAGE_TAG` | コミット SHA 先頭 7 桁 | 上書きしたいときだけ指定 |
| `SKIP_DOCKER_PUSH` | （未設定 = Docker あり） | `true` にすると **Maven ビルドと JAR のみ**（Docker / ECR なし） |

### IAM（CodeBuild のサービスロール）

Docker イメージまで push する場合、ロールに少なくとも次が必要です。

- `sts:GetCallerIdentity`
- `ecr:GetAuthorizationToken`（Resource: `*`）
- 対象リポジトリへの push 系（例: `ecr:BatchCheckLayerAvailability`, `ecr:PutImage`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`）

実務では AWS マネジメントコンソールの **「ECR へのアクセス権限を持つポリシー」** をロールにアタッチする方法が簡単です。

### ビルド成果物

| 出力 | 用途 |
|------|------|
| `target/secapp.jar` | Spring Boot の実行 JAR |
| `imagedefinitions.json` | ECS デプロイ用（コンテナ名 `app` は `docker-compose.yml` のサービス名に合わせています） |
| ECR 上のイメージ | `secapp:<タグ>` として保存（Docker 利用時） |

Maven の依存は `.m2/repository` にキャッシュします。テストは DB 不要（モックのみ）なので、MySQL なしで `mvn test` が通ります。

### ビルドが失敗したとき（フェーズ別）

ログに `ERROR:` と出る行を手がかりにしてください。**失敗するフェーズによって原因が違います。**

| フェーズ | よくある原因 | 対処 |
|--------|--------------|------|
| **PRE_BUILD** | リージョン未設定 | `AWS_DEFAULT_REGION` を CodeBuild に設定 |
| **PRE_BUILD** | Docker が使えない | **特権モード**を有効化。または `SKIP_DOCKER_PUSH=true` |
| **PRE_BUILD** | IAM 不足（ログイン前） | `ecr:GetAuthorizationToken`、`sts:GetCallerIdentity` |
| **BUILD** | テスト・コンパイル失敗 | ログの Maven エラーを修正（ローカルで `mvn test` を再現） |
| **POST_BUILD** | **ECR リポジトリが無い** | `aws ecr create-repository` で作成（名前は `IMAGE_REPO_NAME` と一致） |
| **POST_BUILD** | push 権限不足 | サービスロールに ECR push 権限を追加 |

補足: **ECR リポジトリが無い**と、多くの場合 **POST_BUILD の `docker push` で失敗**します。PRE_BUILD の「レジストリへのログイン」は通っても、push の段階で `RepositoryNotFoundException` などになります。PRE_BUILD で落ちる場合は、上表のリージョン・特権モード・IAM を先に確認してください。

### Docker を使わないビルドだけ試す場合

ECR や Docker の準備がまだのときは、CodeBuild の環境変数に次を設定すると、**`mvn test package` と JAR アーティファクトだけ**実行します。

```
SKIP_DOCKER_PUSH=true
```

### 注意（教材アプリ）

本アプリは**意図的に脆弱な教材**です。[重要な注意](#重要な注意)のとおり、**本番運用には使わない**でください。インターネット公開が必要な場合は [AWS（デモ公開 / CodePipeline）](#awsデモ公開--codepipeline) のデモ構成に限定し、閉じた検証以外では使わないでください。

## 初期ユーザー（両版共通の認証情報）

| ユーザー名 | パスワード   | 役割  |
|------------|--------------|-------|
| admin      | admin123     | ADMIN |
| alice      | wonderland   | USER  |
| bob        | builder      | USER  |

VULNERABLE 側はパスワードを平文のまま `vuln_users` に保存しています。
SECURE 側は同じパスワードを BCrypt ハッシュ化して `sec_users` に保存しています（`DataSeeder` が起動時に投入）。
画面上に一覧・プロフィールへ平文を載せているのは**教材として可視化しやすくするため**であり、実運用では載せません（解説ページ `/docs/auth` にも記載）。

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

/secure/...                上記とほぼ同じパス構成。状態変更系は POST + CSRF（ログアウトも POST）。
                            一覧 /secure/users は ADMIN のみ、コメント・投稿削除も POST 限定。
```

## ディレクトリ構成（重要部分）

```
02_aws-deploy.bat … 09_aws-app-url.bat   AWS デモ公開用ワンクリック（ECS パブリック IP、ALB なし）
scripts/               WSL / Windows / ワンクリック用の起動・停止スクリプト
  one-click-wsl-up.cmd / one-click-wsl-up.ps1   Windows→WSL ワンクリック起動
  one-click-wsl-down.cmd / one-click-wsl-down.ps1  同上の停止
  one-click-wsl-restart.cmd / one-click-wsl-restart.ps1  同上のソース反映（compose up --build app）
  Ensure-DockerDesktop.ps1                      Docker 待機ロジック（他スクリプトから dot-source）
  wsl-up.sh / wsl-down.sh / wsl-restart.sh
  docker-desktop-up.ps1 / docker-desktop-down.ps1
  docker-desktop-up.cmd / docker-desktop-down.cmd
aws/                   AWS デモ公開（CloudFormation, Pipeline 用 buildspec, deploy.env.example, scripts/）
buildspec.yml          単体 CodeBuild 用ビルド定義（Pipeline とは別）
Dockerfile
docker-compose.yml
src/main/java/com/example/secapp/
  common/        共通エンティティ・DTO（両版で共有）
  config/        SecurityConfig（/secure/** 向け厳格チェーン + それ以外向け寛容チェーン）, DataSeeder
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
| 1 | **SQLi（ログイン／検索／コメント）** | `/vulnerable/login` でユーザー名に `' OR 1=1 -- `（末尾スペース付き）、パスワード任意でログイン。**攻撃成功の目安:** 投稿一覧の見出し横に「`admin` でログイン中」と出る。検索 SQLi とコメント経路の SQLi は `/docs/sqli` の手順を参照。 |
| 2 | **蓄積型 XSS** | `alice` / `wonderland` でログイン → 新規投稿の本文に `<script>alert(document.cookie)</script>`。**攻撃成功の目安:** 一覧や詳細でスクリプトが実行されアラートが出る。 |
| 3 | **反射型 XSS** | `/vulnerable/posts` の検索に `<img src=x onerror=alert(1)>` を入力して検索。**攻撃成功の目安:** 一覧表示時に `alert(1)` が動く。 |
| 4 | **CSRF / GET で状態変更** | ログイン後、**`/docs/csrf`** の手順どおり試す（GET 削除の複数パターン、別投稿 ID の GET 更新など）。単純な例だけなら新しいタブのアドレスバーに `/vulnerable/posts/1/delete` のような GET 削除 URL を入力してもよい。**攻撃成功の目安:** 意図せず状態が書き換わる。 |
| 5 | **パスワード平文** | `admin` / `admin123` でログインし、ユーザー一覧またはプロフィールで平文のまま読めることを確認（画面表示は教材用の可視化）。**攻撃成功の目安:** 保存パスワードがそのまま分かる。対策版は BCrypt と画面からの除去。 |
| 6 | **セッション改ざん** | `bob` / `builder` でログイン → DevTools の Cookie で `vuln_uid` を `1` に変更 → 一覧を再読み込み。**攻撃成功の目安:** 表示が `admin` に切り替わる。 |
| 7 | **IDOR / 認可不備** | **`/docs/idor`** に手順をまとめている（例: admin のプロフィールを一般ユーザ権限でメール変更、未ログインでユーザー一覧など）。**一例（メール改ざん）:** `bob` でログインしたまま `http://localhost:8080/vulnerable/users/1/update-email?email=hacked@example.com` にアクセス。**攻撃成功の目安:** 権限や本人性のない操作が成立する。 |

同じ URL・操作を `/secure/...` に置き換えると、パラメータ化クエリ・エスケープ・CSRF・BCrypt・セッション・オーナー判定などにより成立しません。

## 重要な注意

- これは学習用アプリです。本番運用には絶対に使わないでください。
- `/vulnerable/**` の挙動は教育目的で**故意に脆弱**にしてあります。
- **ローカル Docker** ではインターネットに公開しないこと。ローカル環境だけで動かしてください。
- **AWS デモ公開**（[`aws/README.md`](aws/README.md)）は任意の検証用です。タスクのパブリック IP `:8080` へ HTTP で公開され、IP は再起動・デプロイのたびに変わります。脆弱版が外部から触れるため、デモ目的・自己責任に限定してください。
- `docker-compose.yml` / `application.yml` には MySQL の **学習用クレデンシャル**が平文で書かれています（**アプリ接続: `secapp` / `secapp`**、**MySQL root: `root` / `rootpass`**）。教材限定の前提で、**他環境への流用は避けて**ください。
- JDBC URL の `useSSL=false` と `allowPublicKeyRetrieval=true` は、ローカル教材環境の接続優先設定です。本番では TLS を有効にし、接続オプションを環境ポリシーに合わせて見直してください。
- `logging.level.org.springframework.jdbc.core=DEBUG` は教材として SQL を追いやすくするためです。本番では機微情報がログに残るリスクがあるため、通常は INFO 以上へ引き上げます。
