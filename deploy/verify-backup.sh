#!/usr/bin/env bash
# Proves a backup can be restored, without touching the live database: it loads the dump into a
# throwaway PostgreSQL container, checks that the data is really there, and that the photo archive
# is readable. Run it on a schedule; a backup nobody has ever restored is only a hope.
#
#   deploy/verify-backup.sh                       # the newest backup in BACKUP_DIR
#   deploy/verify-backup.sh db-X.dump photos-X.tar.gz
#
# Exit code 0 = the backup is good. Anything else prints why on stderr, so a cron job can mail it.
set -euo pipefail

cd "$(dirname "$0")"

env_value() {
  [ -f .env ] || return 0
  grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'\$//" || true
}
BACKUP_DIR="${BACKUP_DIR:-$(env_value BACKUP_DIR)}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/s2000}"

if [ "$#" -eq 2 ]; then
  DUMP="$(realpath "$1")"
  PHOTOS="$(realpath "$2")"
elif [ "$#" -eq 0 ]; then
  DUMP="$(ls -1t "$BACKUP_DIR"/db-*.dump 2>/dev/null | head -n 1 || true)"
  PHOTOS="$(ls -1t "$BACKUP_DIR"/photos-*.tar.gz 2>/dev/null | head -n 1 || true)"
else
  echo "Usage: $0 [<db-....dump> <photos-....tar.gz>]" >&2
  exit 2
fi
[ -n "$DUMP" ] && [ -s "$DUMP" ] || { echo "No database backup found (looked in $BACKUP_DIR)." >&2; exit 1; }
[ -n "$PHOTOS" ] && [ -s "$PHOTOS" ] || { echo "No photo backup found (looked in $BACKUP_DIR)." >&2; exit 1; }

# A backup that is days old means the nightly job has stopped; say so even if the file is intact.
MAX_AGE_HOURS="${BACKUP_MAX_AGE_HOURS:-$(env_value BACKUP_MAX_AGE_HOURS)}"
MAX_AGE_HOURS="${MAX_AGE_HOURS:-36}"
if [ "$#" -eq 0 ] && [ -n "$(find "$DUMP" -mmin +$((MAX_AGE_HOURS * 60)) 2>/dev/null)" ]; then
  echo "The newest backup is older than $MAX_AGE_HOURS hours: $DUMP. Is the nightly backup running?" >&2
  exit 1
fi

NAME="s2000-verify-$$"
cleanup() { docker rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

docker run -d --name "$NAME" -e POSTGRES_PASSWORD=verify -e POSTGRES_DB=verify postgres:16-alpine >/dev/null
# The image starts a temporary server to create the database, stops it, and starts the real one; the
# real one is up when "ready to accept connections" has appeared twice in the log.
ready=0
for _ in $(seq 1 60); do
  if [ "$(docker logs "$NAME" 2>&1 | grep -c 'database system is ready to accept connections')" -ge 2 ] \
    && docker exec "$NAME" pg_isready -U postgres -d verify >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 1
done
[ "$ready" -eq 1 ] || { echo "Throwaway database did not start." >&2; exit 1; }

docker exec -i "$NAME" pg_restore -U postgres -d verify --no-owner --exit-on-error < "$DUMP" \
  || { echo "pg_restore failed: the dump is damaged or incomplete: $DUMP" >&2; exit 1; }

count() { docker exec "$NAME" psql -U postgres -d verify -tA -c "select count(*) from $1"; }
MEMBERS="$(count member)"
COACHES="$(count coach_account)"
CHANGESETS="$(count databasechangelog)"
[ "$COACHES" -ge 1 ] || { echo "Restored database has no coach account: the dump looks empty." >&2; exit 1; }
[ "$CHANGESETS" -ge 1 ] || { echo "Restored database has no schema history." >&2; exit 1; }

# Every photo row must have a file in the archive: a row pointing at nothing is a broken photo.
TMP="$(mktemp -d)"
trap 'cleanup; rm -rf "$TMP"' EXIT
tar -tzf "$PHOTOS" > "$TMP/files.txt" || { echo "The photo archive cannot be read: $PHOTOS" >&2; exit 1; }
docker exec "$NAME" psql -U postgres -d verify -tA -c \
  "select storage_key from progress_photo union all select storage_key from success_story_photo union all select storage_key from case_study_photo" \
  | sort -u > "$TMP/keys.txt"
MISSING=0
while IFS= read -r key; do
  [ -n "$key" ] || continue
  grep -qF "/$key" "$TMP/files.txt" || { echo "Photo file missing from the archive: $key" >&2; MISSING=$((MISSING + 1)); }
done < "$TMP/keys.txt"
[ "$MISSING" -eq 0 ] || { echo "$MISSING photo file(s) referenced by the database are not in the archive." >&2; exit 1; }

echo "Backup OK: $(basename "$DUMP") restored ($MEMBERS members, $CHANGESETS schema steps), photo archive readable ($(wc -l < "$TMP/keys.txt") photo files referenced and present)."
