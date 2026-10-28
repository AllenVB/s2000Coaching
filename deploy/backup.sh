#!/usr/bin/env bash
# Backs up what cannot be rebuilt: the database and the members' photos.
#
#   deploy/backup.sh
#
# Writes db-<time>.dump and photos-<time>.tar.gz into BACKUP_DIR and deletes the ones older
# than BACKUP_KEEP_DAYS. A copy that stays on the same machine is not a backup: copy BACKUP_DIR
# somewhere else too (see docs/yayin-rehberi.md). The files hold health data; keep them private.
set -euo pipefail

cd "$(dirname "$0")"
COMPOSE=(docker compose -f docker-compose.yml)

# Reads one value from .env without running it: .env is a list of settings for Docker, not a
# shell script, and a value such as `ADMIN_NAME=Ali Veli` or one holding `&` or `$` would be
# taken for a command.
env_value() {
  [ -f .env ] || return 0
  grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'\$//" || true
}

BACKUP_DIR="${BACKUP_DIR:-$(env_value BACKUP_DIR)}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/s2000}"
KEEP_DAYS="${BACKUP_KEEP_DAYS:-$(env_value BACKUP_KEEP_DAYS)}"
KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$BACKUP_DIR"
umask 077

# Written to a temporary name and renamed at the end, so a half-written file is never mistaken
# for a good one.
"${COMPOSE[@]}" exec -T postgres pg_dump -U s2000coaching -d s2000coaching --format=custom \
  > "$BACKUP_DIR/db-$STAMP.dump.partial"
"${COMPOSE[@]}" exec -T backend tar -czf - -C /data photos \
  > "$BACKUP_DIR/photos-$STAMP.tar.gz.partial"

# An empty dump means pg_dump failed quietly; refuse to call that a backup.
for partial in "$BACKUP_DIR"/*-"$STAMP".*.partial; do
  [ -s "$partial" ] || { echo "Backup file is empty: $partial" >&2; rm -f "$BACKUP_DIR"/*-"$STAMP".*.partial; exit 1; }
done
mv "$BACKUP_DIR/db-$STAMP.dump.partial" "$BACKUP_DIR/db-$STAMP.dump"
mv "$BACKUP_DIR/photos-$STAMP.tar.gz.partial" "$BACKUP_DIR/photos-$STAMP.tar.gz"

find "$BACKUP_DIR" -maxdepth 1 -type f \( -name 'db-*.dump' -o -name 'photos-*.tar.gz' \) \
  -mtime +"$KEEP_DAYS" -delete

echo "Backup written: $BACKUP_DIR/db-$STAMP.dump and photos-$STAMP.tar.gz"
