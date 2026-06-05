#!/bin/bash
# ============================================================
# Pawn Shine — PRIVATE Instance Setup Script
# Run on: Private subnet VM (NO public IP)
# Purpose: Runs the Spring Boot backend and PostgreSQL database
#          inside Docker containers.
#
# WHY a separate private instance for backend + DB?
#   1. SECURITY: No public IP means zero direct attack surface
#      from the internet. The DB is double-protected — inside
#      Docker AND behind a private subnet.
#   2. DATA PROTECTION: Database credentials and data never
#      traverse the public internet. All traffic stays within
#      the Oracle VCN.
#   3. BLAST RADIUS: If the public instance is compromised,
#      attackers cannot directly reach the database. They'd
#      need to pivot through the VCN, which OCI security
#      lists can further restrict.
#   4. COMPLIANCE: Many regulations (PCI-DSS, GDPR) recommend
#      or require databases to be on non-public networks.
# ============================================================
set -euo pipefail

DEPLOY_DIR=/home/ubuntu/pawn-deploy

echo "============================================================"
echo "  Pawn Shine — PRIVATE Instance Setup (Backend + DB)"
echo "============================================================"

# -----------------------------------------------------------
# 1. Update and Upgrade System Packages
# -----------------------------------------------------------
echo "[1/8] Updating and upgrading system packages..."
sudo apt-get update && sudo apt-get upgrade -y

# -----------------------------------------------------------
# 2. Install Docker and utilities
#
# WHY Docker for backend + DB?
#   - PostgreSQL runs in an isolated container with persistent
#     volumes — easy to backup, restore, and upgrade.
#   - Spring Boot builds are reproducible — same JDK, same
#     Maven version, every time.
#   - Container restart policies ensure auto-recovery on crash.
# -----------------------------------------------------------
echo "[2/8] Installing Docker and utilities..."
sudo apt-get install -y docker.io docker-compose-v2 git curl wget unzip iptables-persistent netfilter-persistent

# -----------------------------------------------------------
# 3. Enable Docker and add user to docker group
# -----------------------------------------------------------
echo "[3/8] Enabling Docker service..."
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ubuntu

# -----------------------------------------------------------
# 4. Configure firewall
#
# WHY restrict to VCN CIDR (10.0.0.0/16)?
#   Port 8080 is ONLY accessible from within the Oracle VCN.
#   This means ONLY the public instance (and other VCN hosts)
#   can reach the backend. Even if someone scans the private
#   IP, they'd need to be inside the VCN first.
#
# WHY no ports 80/443?
#   This instance doesn't serve web traffic directly. Only
#   the public instance's Nginx handles external HTTP requests.
# -----------------------------------------------------------
echo "[4/8] Configuring firewall (port 8080, VCN-only access)..."
# Allow backend port 8080 ONLY from VCN internal network
sudo iptables -I INPUT 6 -m state --state NEW -p tcp -s 10.0.0.0/16 --dport 8080 -j ACCEPT
# Block 8080 from all other sources (defense in depth — private subnet already blocks this)
sudo iptables -A INPUT -p tcp --dport 8080 -j DROP
sudo netfilter-persistent save

# -----------------------------------------------------------
# 5. Create deployment directory
# -----------------------------------------------------------
echo "[5/8] Creating deployment directory..."
mkdir -p "$DEPLOY_DIR/backups"
cd "$DEPLOY_DIR"

# -----------------------------------------------------------
# 6. Clone backend repository
#
# WHY only the backend repo? This instance doesn't need
# frontend source code. Keeps the deploy lean and focused.
# -----------------------------------------------------------
echo "[6/8] Cloning backend repository..."
if [ ! -d "pawn-backend" ]; then
    git clone https://github.com/SAIPREMKAKUMANI/pawn-shine-backend.git pawn-backend
else
    echo "  pawn-backend already exists, skipping clone."
fi

# -----------------------------------------------------------
# 7. Copy configuration files and set up environment
# -----------------------------------------------------------
echo "[7/8] Setting up configuration files..."
cp "$DEPLOY_DIR/pawn-backend/docker-compose.private.yml" "$DEPLOY_DIR/docker-compose.yml"

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

# Copy utility scripts
cp "$DEPLOY_DIR/pawn-backend/backup.sh" "$DEPLOY_DIR/"
cp "$DEPLOY_DIR/pawn-backend/update.sh" "$DEPLOY_DIR/"
chmod +x "$DEPLOY_DIR/backup.sh"
chmod +x "$DEPLOY_DIR/update.sh"

# -----------------------------------------------------------
# 8. Set up daily backup cron job (2 AM)
#
# WHY backups on the private instance?
#   The database lives here. Backups are done locally to avoid
#   sending data over the network. The pg_dump runs inside the
#   Docker container for direct access.
# -----------------------------------------------------------
echo "[8/8] Setting up daily backup cron job (2 AM)..."
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
    ║  4. View logs:                                            ║
    ║     docker compose logs -f                                ║
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
