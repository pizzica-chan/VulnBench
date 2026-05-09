#!/usr/bin/env bash
# VulnBench / SecApp — アプリ Dockerfile をビルドし直して app コンテナを載せ替える（ソース反映用）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "error: docker が見つかりません。" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "error: \`docker compose\` が使えません（Docker Compose V2 が必要です）。" >&2
  exit 1
fi

echo "==> repo: $REPO_ROOT"
echo "==> docker compose up -d --build --force-recreate app"
echo "    （MySQL はそのまま。DB まで初期化し直す場合は wsl-up.sh を使う）"
docker compose up -d --build --force-recreate app

echo ""
echo "==> ビルド・載せ替え後のコンテナ状態:"
docker compose ps
echo ""
echo "  URL: http://localhost:8080"
echo "  ログ: cd \"$REPO_ROOT\" && docker compose logs -f app"
echo "  初期化込みの再作成: \"$SCRIPT_DIR/wsl-up.sh\""
