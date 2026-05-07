#!/usr/bin/env bash
# VulnBench / SecApp — WSL から Docker Compose を停止する
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "error: docker が見つかりません。" >&2
  exit 1
fi

echo "==> repo: $REPO_ROOT"
echo "==> docker compose down"
docker compose down

echo "==> 停止しました。"
