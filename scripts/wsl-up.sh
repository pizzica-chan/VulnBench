#!/usr/bin/env bash
# VulnBench / SecApp — WSL から Docker Compose で MySQL + アプリを起動する
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "error: docker が見つかりません。" >&2
  echo "  Docker Desktop をインストールし、[Settings] → [Resources] → [WSL integration] でこのディストリを有効にしてください。" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "error: \`docker compose\` が使えません（Docker Compose V2 が必要です）。" >&2
  exit 1
fi

echo "==> repo: $REPO_ROOT"
echo "==> docker compose up --build -d"
docker compose up --build -d

echo ""
echo "==> 起動しました。コンテナ状態:"
docker compose ps
echo ""
echo "  URL: http://localhost:8080"
echo "  ログ: cd \"$REPO_ROOT\" && docker compose logs -f app"
echo "  停止: \"$SCRIPT_DIR/wsl-down.sh\""
