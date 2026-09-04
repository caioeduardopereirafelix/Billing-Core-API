#!/bin/sh

set -eu

DB_HOST="${DB_HOST:-postgres}"
DB_USER="${DB_USER:-billing}"
DB_NAME="${DB_NAME:-billing}"
INTERVAL="${BACKUP_INTERVAL_SECONDS:-86400}"
KEEP="${BACKUP_KEEP:-7}"
OUT_DIR=/backups

mkdir -p "$OUT_DIR"

while true; do
	ts=$(date -u +%Y%m%dT%H%M%SZ)
	file="$OUT_DIR/${DB_NAME}-${ts}.sql.gz"
	echo "[backup] $(date -u) -> $file"
	if pg_dump -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" | gzip > "$file.tmp"; then
		mv "$file.tmp" "$file"
		echo "[backup] ok ($(du -h "$file" | cut -f1))"
	else
		echo "[backup] FAILED"
		rm -f "$file.tmp"
	fi


	ls -1t "$OUT_DIR"/${DB_NAME}-*.sql.gz 2>/dev/null | tail -n +"$((KEEP + 1))" | while read -r old; do
		echo "[backup] rotating out $old"
		rm -f "$old"
	done

	sleep "$INTERVAL"
done
