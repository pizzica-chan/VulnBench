# secapp AWS デモ環境

SecApp 向けの個人デモ用 AWS 構成です。ECS Fargate タスクの **パブリック IP** 経由でインターネットから HTTP アクセスできます（**ALB なし**でコスト抑制）。

## 概要

| 項目 | 内容 |
|------|------|
| 用途 | デモ・検証（インターネット公開） |
| 公開 URL | `http://{タスクのパブリック IP}:8080`（`09_aws-app-url.bat` で表示） |
| アプリ | Spring Boot 3（JDK 21）、ポート 8080 |
| DB | 同一 ECS タスク内の **MySQL 8 サイドカー**（永続化なし・再起動でリセット） |
| 配備 | CodePipeline（Build → Test → **ECS ローリング**） |
| 初期タスク数 | `DesiredCount: 0`（手動で 1 にしたときだけ Fargate 課金） |

## アーキテクチャ

```text
インターネット ──► ECS Fargate タスク (1 台) … パブリック IP :8080
                      ├─ app (Spring Boot) :8080  ◄── 直接公開
                      └─ mysql              :3306  ◄── localhost のみ（外部非公開）

GitHub ──► CodePipeline
              ├─ Build  (CodeBuild)  … Docker ビルド → ECR プッシュ
              ├─ Test   (CodeBuild)  … mvn test
              └─ Deploy (Amazon ECS) … imagedefinitions.json
```

- ECS タスク SG: インバウンド **TCP 8080** を全世界に許可（MySQL は直接公開しない）
- **ALB は使いません**（停止中 `desired-count=0` でも ALB 固定費がかからない）
- タスク再起動・デプロイのたびに **パブリック IP は変わります**（`09_aws-app-url.bat` で都度確認）
- ルートの `buildspec.yml` は **この Pipeline では使いません**（別の CodeBuild プロジェクト用に残しています）。Pipeline は `aws/buildspec-build.yml` と `aws/buildspec-test.yml` を使用します。

## ディレクトリ構成

```text
secure/
├── 02_aws-deploy.bat            ワンクリック CloudFormation デプロイ
├── 03_aws-pipeline-run.bat      Pipeline 手動実行（Git push では自動起動しない）
├── 04_aws-ecs-start.bat         ECS 起動 (desired-count=1)
├── 05_aws-ecs-stop.bat          ECS 停止 (desired-count=0)
├── 06_aws-stack-delete.bat      スタック完全削除
├── 07_aws-mysql-shell.bat       ECS MySQL クライアント（ECS Exec）
├── 08_aws-mysql-portforward.bat ECS MySQL ポートフォワード (localhost:13306)
├── 09_aws-app-url.bat           タスクのパブリック IP / URL 表示
aws/
├── README.md                    本ファイル
├── deploy.env.example           デプロイ用パラメータ雛形
├── deploy.env                   ローカル用（Git 未追跡）
├── buildspec-build.yml          Pipeline Build ステージ
├── buildspec-test.yml           Pipeline Test ステージ
├── cloudformation/
│   └── demo-stack.yaml          インフラ + Pipeline 一式
└── scripts/
    ├── deploy-stack.ps1
    ├── run-pipeline.ps1
    ├── delete-stack.ps1
    ├── ecs-scale.ps1
    ├── show-app-url.ps1
    ├── mysql-shell.ps1
    ├── mysql-port-forward.ps1
    └── port-forward.ps1
```

## 前提

- AWS CLI v2（プロファイル・リージョン設定済み）
- GitHub リポジトリと **AWS CodeConnections**（旧 CodeStar Connections）連携
- 必要 IAM 権限の例: CloudFormation、ECS、ECR、EC2（ENI 参照）、CodePipeline、CodeBuild、IAM ロール作成、S3、Logs

## 初回セットアップ

### 1. GitHub 連携（CodeConnections）

2024年3月以降、**CodeStar Connections は AWS CodeConnections に名称変更** されています。機能は同じで、CloudFormation や CLI の ARN 形式（`codestar-connections`）も引き続き使えます。

**コンソールで開く:**

1. 直接 URL: [Developer Tools → Settings → Connections](https://console.aws.amazon.com/codesuite/settings/connections)
2. または AWS コンソール検索バーで **「Connections」** または **「CodeConnections」** と検索

**新規作成（GitHub）:**

1. Connections 画面 → **Create connection**
2. Provider: **GitHub** → 名前を付けて作成
3. **Connect to GitHub** で GitHub App をインストールし、対象リポジトリへのアクセスを許可
4. ステータスが **Available** になるまで待つ
5. 表示される **Connection ARN** を `deploy.env` の `GITHUB_CONNECTION_ARN` に設定

**CLI で一覧:**

```powershell
aws codeconnections list-connections --region ap-northeast-1
```

### 2. deploy.env を作成

```powershell
copy aws\deploy.env.example aws\deploy.env
```

`aws/deploy.env` を編集します。

| 変数 | 必須 | 説明 |
|------|------|------|
| `GITHUB_CONNECTION_ARN` | ◎ | CodeConnections ARN |
| `REPOSITORY_ID` | ◎ | 例: `pizzica-chan/VulnBench` |
| `AWS_REGION` | △ | 省略時 `ap-northeast-1` |
| `BRANCH_NAME` | △ | 省略時 `master` |
| `STACK_NAME` / `PROJECT_NAME` | △ | 省略時 `secapp-demo` |

### 3. CloudFormation スタック作成

**ワンクリック（Windows）:** リポジトリ直下の `02_aws-deploy.bat` をダブルクリック

**PowerShell:**

```powershell
.\aws\scripts\deploy-stack.ps1
```

### 4. Pipeline 実行

**Git push では Pipeline は自動起動しません**（CodeBuild 課金抑制のため `DetectChanges: false`）。

デプロイしたいときだけ手動実行します。

**ワンクリック（Windows）:** `03_aws-pipeline-run.bat`

**AWS コンソール:** CodePipeline → `secapp-demo-pipeline` → 「変更をリリース」

Build → Test → Deploy が成功するまで待ちます。

### 5. ECS タスク起動

**ワンクリック（Windows）:** `04_aws-ecs-start.bat`

タスクが RUNNING になるまで 2〜3 分待ちます。

### 6. アクセス

**ワンクリック（Windows）:** `09_aws-app-url.bat`

出力例: `http://54.XXX.XXX.XXX:8080/`

ブラウザでその URL を開きます。デモ用アカウントは初回起動時にシードされます（ルート [README.md](../README.md) を参照）。

**既存スタックを ALB 構成から移行する場合:** `02_aws-deploy.bat` を再実行してスタックを更新してください（ALB が削除され、ECS がパブリック IP 公開に切り替わります）。

## 課金について

**Fargate はタスク RUNNING 中のみ課金**されます（ALB 固定費なし）。

| 状態 | 主な課金 |
|------|----------|
| `desired-count=0` | Pipeline / S3 / ECR 等の少額のみ |
| `desired-count=1` | Fargate **約 $15–20/月** + 上記 |
| Pipeline / CodeBuild 実行時 | ビルド時間に応じた従量 |

### 使わないとき（課金抑制）

**ワンクリック（Windows）:** `05_aws-ecs-stop.bat`（Fargate 停止） / `06_aws-stack-delete.bat`（完全削除）

Fargate だけ止める場合は `05_aws-ecs-stop.bat`。スタックごと消す場合は `06_aws-stack-delete.bat` です。

## セキュリティに関する注意

本アプリは **意図的に脆弱な学習用アプリ** を **HTTP のみ** で公開するデモ向け構成です。WAF や IP 制限は未設定です。**本番運用には使用しないでください。** インターネット公開はデモ目的の自己責任で行ってください。

## トラブルシューティング

### URL にアクセスできない

- `desired-count` が 1 か、タスクが RUNNING か確認
- `09_aws-app-url.bat` で **最新のパブリック IP** を確認（デプロイ・再起動で IP は変わる）
- CloudWatch Logs `/ecs/secapp-demo` の `app` ストリームを確認
- 初回は Pipeline 成功後に ECR に `latest` イメージがあるか確認

### アプリが起動しない（DB 接続）

- MySQL サイドカーが healthy になってから app が起動します（タスク定義の `DependsOn`）
- CloudWatch Logs `/ecs/secapp-demo` の `app` ストリームで Flyway / JDBC エラーを確認

### ECS MySQL に接続する

前提: タスク RUNNING（`04_aws-ecs-start.bat`）、[Session Manager Plugin](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html) インストール済み。

**方法 A — ECS Exec（対話）:** `07_aws-mysql-shell.bat`

bat 実行で `secapp` に MySQL クライアントが直接開きます。終了は `exit` または `\q`。

**方法 B — ローカル mysql クライアント:** `08_aws-mysql-portforward.bat`

1. bat を起動したまま（`Ctrl+C` で停止）
2. 別ターミナル:

```powershell
mysql -h 127.0.0.1 -P 13306 -uroot -p secapp
```

| 項目 | 値 |
|------|-----|
| DB 名 | `secapp` |
| ユーザー | `root` |
| パスワード | `deploy.env` の `MYSQL_ROOT_PASSWORD`（未設定時 `secapp_demo_pass`） |

DB は永続化なし。タスク再起動でデータは消え、Flyway マイグレーションとシーダが再実行されます。

## ローカル Docker との違い

| 項目 | ローカル | AWS デモ |
|------|----------|----------|
| DB | tmpfs（再起動でリセット） | タスク内 MySQL・再起動で消える |
| アクセス | `http://localhost:8080` | `http://{タスクのパブリック IP}:8080` |
| 配備 | `docker compose` | CodePipeline → ECS |

ローカル開発は引き続き `docker-compose.yml` や `scripts/one-click-wsl-up.cmd` を使用してください。
