#!/usr/bin/env bash
# Puts a backup back. This REPLACES the current database and photos with the backup's.
#
#   deploy/restore.sh /var/backups/s2000/db-20261028-030000.dump /var/backups/s2000/photos-20261028-030000.tar.gz
#
# Stops the backend while it works and starts it again afterwards.
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <db-....dump> <photos-....tar.gz>" >&2
  exit 2
fi
DUMP="$(realpath "$1")"
PHOTOS="$(realpath "$2")"
[ -s "$DUMP" ] && [ -s "$PHOTOS" ] || { echo "Backup file missing or empty." >&2; exit 2; }

cd "$(dirname "$0")"
COMPOSE=(docker compose -f docker-compose.yml)

echo "This will REPLACE the database and photos on this server with:"
echo "  $DUMP"
echo "  $PHOTOS"
read -r -p 'Type EVET to continue: ' answer
[ "$answer" = "EVET" ] || { echo "Cancelled."; exit 1; }

"${COMPOSE[@]}" stop backend

# --clean --if-exists drops each object before recreating it, so it works on a database that
# already holds data as well as on an empty one.
"${COMPOSE[@]}" exec -T postgres pg_restore -U s2000coaching -d s2000coaching \
  --clean --if-exists --no-owner --exit-on-error < "$DUMP"

# The photos volume is emptied first, so files deleted since the backup do not come back to life
# by accident, and files added since do not linger without a database row.
"${COMPOSE[@]}" run --rm --no-deps -T --entrypoint bash backend -c \
  'find /data/photos -mindepth 1 -delete && tar -xzf - -C /data' < "$PHOTOS"

"${COMPOSE[@]}" start backend
echo "Restored. The backend is starting; check:  docker compose -f deploy/docker-compose.yml logs -f backend"
