#!/bin/bash
# ============================================================
# Pawn Shine — PRIVATE Instance Setup Script
# Run on: Private subnet VM (NO public IP)
#
# This is the ONLY setup script in the backend repo.
# For the PUBLIC instance, use setup-public.sh from the
# frontend repo (pawn-shine-portal).
#
# Usage:
#   ./setup-vm.sh
#   # or directly:
#   ./setup-private.sh
#
# ARCHITECTURE:
# ┌────────────────────┐        ┌─────────────────────────┐
# │  PUBLIC INSTANCE   │        │  PRIVATE INSTANCE ←YOU  │
# │  (pawn-shine-portal│        │  (pawn-shine-backend)   │
# │   repo)            │        │                         │
# │                    │        │                         │
# │  Nginx (UI)        │──VCN──▶│  Spring Boot + Postgres │
# │  :80 / :443        │        │  :8080 (VCN only)       │
# └────────────────────┘        └─────────────────────────┘
# ============================================================
set -euo pipefail

DEPLOY_DIR=/home/ubuntu/pawn-deploy
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"

echo "============================================================"
echo "  Pawn Shine — PRIVATE Instance Setup (Backend + DB)"
echo "============================================================"

# -----------------------------------------------------------
# 1. Update and Upgrade System Packages
# -----------------------------------------------------------
echo "[1/7] Updating and upgrading system packages..."
sudo apt-get update && sudo apt-get upgrade -y

# -----------------------------------------------------------
# 2. Install Docker and utilities
# -----------------------------------------------------------
echo "[2/7] Installing Docker and utilities..."
sudo apt-get install -y docker.io docker-compose-v2 git curl wget unzip iptables-persistent netfilter-persistent

# -----------------------------------------------------------
# 3. Enable Docker and add user to docker group
# -----------------------------------------------------------
echo "[3/7] Enabling Docker service..."
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ubuntu

# -----------------------------------------------------------
# 4. Create deployment directory
# -----------------------------------------------------------
echo "[4/7] Creating deployment directory..."
mkdir -p "$DEPLOY_DIR/backups"
cd "$DEPLOY_DIR"

# -----------------------------------------------------------
# 6. Clone backend repository (ONLY backend — no frontend)
# -----------------------------------------------------------
echo "[5/7] Cloning backend repository (Branch: $DEPLOY_BRANCH)..."
if [ ! -d "pawn-backend" ]; then
    git clone -b "$DEPLOY_BRANCH" https://github.com/SAIPREMKAKUMANI/pawn-shine-backend.git pawn-backend
else
    echo "  pawn-backend already exists, skipping clone."
fi

# -----------------------------------------------------------
# 7. Copy configuration files and set up environment
# -----------------------------------------------------------
echo "[6/7] Setting up configuration files..."
cp "$DEPLOY_DIR/pawn-backend/docker-compose.yml" "$DEPLOY_DIR/docker-compose.yml"

# Open port 8080 in the firewall (VCN only, no public IP)
sudo iptables -I INPUT 1 -p tcp --dport 8080 -j ACCEPT
sudo netfilter-persistent save

# Create .env from template if it doesn't exist
if [ ! -f "$DEPLOY_DIR/.env" ]; then
    cp "$DEPLOY_DIR/pawn-backend/.env.example" "$DEPLOY_DIR/.env"
    echo ""
    echo "  ╔══════════════════════════════════════════════════════╗"
    echo "  ║  ⚠️  ACTION REQUIRED: Edit .env with real secrets    ║"
    echo "  ║  nano ~/pawn-deploy/.env                             ║"
    echo "  ╚══════════════════════════════════════════════════════╝"
    echo ""
fi

# Copy backend-only utility scripts
cp "$DEPLOY_DIR/pawn-backend/backup.sh" "$DEPLOY_DIR/"
cp "$DEPLOY_DIR/pawn-backend/update.sh" "$DEPLOY_DIR/"
chmod +x "$DEPLOY_DIR/backup.sh"
chmod +x "$DEPLOY_DIR/update.sh"

# -----------------------------------------------------------
# 7. Set up daily backup cron job (2 AM)
# -----------------------------------------------------------
echo "[7/7] Setting up daily backup cron job (2 AM)..."
(crontab -l 2>/dev/null; echo '0 2 * * * /home/ubuntu/pawn-deploy/backup.sh >> /home/ubuntu/pawn-deploy/backups/backup.log 2>&1') | crontab -

# -----------------------------------------------------------
# Write MOTD
# -----------------------------------------------------------
sudo bash -c "cat > /etc/motd << 'MOTD'

    ╔═══════════════════════════════════════════════════════════╗
    ║        🏆 Pawn Shine — PRIVATE Instance (Backend+DB)      ║
    ╠═══════════════════════════════════════════════════════════╣
    ║                                                           ║
    ║  Role: Spring Boot Backend + PostgreSQL Database           ║
    ║  ⚠️  This instance has NO public IP                        ║
    ║                                                           ║
    ║  1. Edit secrets:                                         ║
    ║     nano ~/pawn-deploy/.env                               ║
    ║                                                           ║
    ║  2. Build & start:                                        ║
    ║     cd ~/pawn-deploy && docker compose up -d --build      ║
    ║                                                           ║
    ║  3. Check status:                                         ║
    ║     docker compose ps                                     ║
    ║                                                           ║
    ║  4. Update & redeploy:                                    ║
    ║     ~/pawn-deploy/update.sh                               ║
    ║                                                           ║
    ║  5. Manual backup:                                        ║
    ║     ~/pawn-deploy/backup.sh                               ║
    ║                                                           ║
    ╚═══════════════════════════════════════════════════════════╝

MOTD"

echo ""
echo "============================================================"
echo "  PRIVATE Instance Setup Complete!"
echo ""
echo "  Next steps:"
echo "    1. Edit secrets: nano ~/pawn-deploy/.env"
echo "    2. Build & start: cd ~/pawn-deploy && docker compose up -d --build"
echo "    3. Verify health: docker compose ps"
echo ""
echo "  Rebooting to apply docker group changes..."
echo "============================================================"
sudo reboot
