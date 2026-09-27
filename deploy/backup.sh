#!/usr/bin/env bash
# (11부) DB 백업. 매일 새벽 cron으로 실행해요.
#   crontab -e 에 추가:
#   0 4 * * * /bin/bash /home/ubuntu/nfcreview/deploy/backup.sh >> /home/ubuntu/nfcreview/deploy/backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")"          # 이 스크립트가 있는 폴더(deploy)로 이동
mkdir -p backups

file="backups/nfcreview-$(date +%Y%m%d-%H%M).sql.gz"
trap 'rm -f "$file"' ERR      # 중간에 실패하면 반쪽짜리 파일을 남기지 않아요

docker compose exec -T db pg_dump -U nfcreview nfcreview | gzip > "$file"

# 14일 지난 백업은 삭제 (디스크 절약)
find backups -name 'nfcreview-*.sql.gz' -mtime +14 -delete

echo "$(date '+%F %T') 백업 완료: $file ($(du -h "$file" | cut -f1))"
