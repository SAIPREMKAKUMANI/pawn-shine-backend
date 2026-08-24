#!/bin/bash
# ============================================================
# Pawn Shine — Daily Backup Script
# Backs up PostgreSQL database + uploaded images
#
# Crontab (run daily at 2 AM):
#   0 2 * * * ~/pawn-deploy/backup.sh >> ~/pawn-deploy/backups/backup.log 2>&1
# ============================================================
set -euo pipefail

TIMESTAMP=$(date +%F_%H-%M-%S)
BACKUP_DIR=~/pawn-deploy/backups
mkdir -p "$BACKUP_DIR"

echo "[$TIMESTAMP] Starting backup..."

# -----------------------------------------------------------
# 1. PostgreSQL database dump
# -----------------------------------------------------------
echo "  Dumping database..."
docker exec pawn_db pg_dump -U postgres pawnbroking | gzip > "$BACKUP_DIR/db_$TIMESTAMP.sql.gz"
echo "  Database backup: db_$TIMESTAMP.sql.gz ($(du -h "$BACKUP_DIR/db_$TIMESTAMP.sql.gz" | cut -f1))"

# -----------------------------------------------------------
# 2. Images volume backup
# -----------------------------------------------------------
echo "  Backing up images volume..."
docker run --rm \
    -v pawn-deploy_pawn_images:/data:ro \
    -v "$BACKUP_DIR":/backup \
    alpine tar czf /backup/images_$TIMESTAMP.tar.gz -C /data .
echo "  Images backup: images_$TIMESTAMP.tar.gz ($(du -h "$BACKUP_DIR/images_$TIMESTAMP.tar.gz" | cut -f1))"

# -----------------------------------------------------------
# 3. Cleanup old backups (keep last 7 days)
# -----------------------------------------------------------
echo "  Cleaning up backups older than 7 days..."
find "$BACKUP_DIR" -name "db_*.sql.gz" -mtime +7 -delete
find "$BACKUP_DIR" -name "images_*.tar.gz" -mtime +7 -delete

echo "[$TIMESTAMP] Backup complete."
