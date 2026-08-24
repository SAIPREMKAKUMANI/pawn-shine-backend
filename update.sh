#!/bin/bash
# ============================================================
# Pawn Shine — Backend Update & Redeploy Script
# Run on: PRIVATE instance only
#
# Pulls latest backend code, rebuilds backend + DB containers,
# and restarts. Runs a backup before updating.
# Data volumes (pgdata, pawn_images) are preserved.
# ============================================================
set -euo pipefail

DEPLOY_DIR=~/pawn-deploy
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
cd "$DEPLOY_DIR"

echo "========================================"
echo "  Pawn Shine — Backend Update"
echo "  Instance: PRIVATE (Backend + DB)"
echo "========================================"

# -----------------------------------------------------------
# 1. Run backup before updating
# -----------------------------------------------------------
echo "[1/4] Running pre-update backup..."
bash backup.sh || echo "  Warning: Backup failed, continuing with update..."

# -----------------------------------------------------------
# 2. Pull latest backend code
# -----------------------------------------------------------
echo "[2/4] Pulling latest backend code from branch: $DEPLOY_BRANCH..."
cd pawn-backend
git fetch origin
git checkout "$DEPLOY_BRANCH"
git pull origin "$DEPLOY_BRANCH"
cd ..

# -----------------------------------------------------------
# 3. Update docker-compose from backend repo
# -----------------------------------------------------------
echo "[3/4] Updating docker-compose.yml..."
cp pawn-backend/docker-compose.yml docker-compose.yml

# -----------------------------------------------------------
# 4. Rebuild and restart backend + DB containers
# -----------------------------------------------------------
echo "[4/4] Rebuilding and restarting backend + DB..."
docker compose up -d --build --force-recreate

# Cleanup dangling images
docker image prune -f

echo ""
echo "========================================"
echo "  Backend update complete!"
echo "  docker compose ps:"
docker compose ps
echo "========================================"
