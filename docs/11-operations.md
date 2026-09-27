# 11부. 운영

> 목표: 서버가 오래 안 죽게, 죽으면 빨리 알고 빨리 살리게, 데이터를 잃지 않게. 그리고 드디어 빨간주막의 실제 카드를 바꾼다.

## 1. 모니터링: 죽으면 바로 알기

UptimeRobot 같은 무료 모니터링 서비스에 가입해서:
- 모니터 종류 HTTP(s), 주소 `https://내도메인/actuator/health`, 간격 5분
- 알림: 이메일 + 폰 앱 푸시

원리는 단순해요. 밖에서 5분마다 찔러보고, 대답이 없거나 `UP`이 아니면 알려줘요. 손님이 "카드가 안 돼요"라고 말하기 전에 내가 먼저 알게 돼요.

## 2. 로그 보기 (서버)

```bash
cd ~/nfcreview/deploy
docker compose logs --tail 200 app          # 최근 200줄
docker compose logs -f app                  # 실시간
docker compose logs app | grep ERROR        # 에러만
```
로그는 `compose.yaml`의 logging 설정대로 10MB × 3개까지만 보관돼요. 이 설정이 없으면 로그가 끝없이 쌓여서 어느 날 디스크가 가득 차 서버가 멈춰요. 실제로 흔한 장애예요.

`기록 실패 (손님 이동은 정상 진행)` ERROR는 손님에겐 영향이 없었다는 뜻이지만, 통계가 빠졌다는 뜻이니 원인을 확인해요.

## 3. 백업: 데이터 잃지 않기 (서버)

```bash
cd ~/nfcreview/deploy
bash backup.sh              # 수동 실행 → backups/ 폴더에 .sql.gz 파일
ls -lh backups
```
매일 새벽 4시 자동 실행:
```bash
crontab -e      # 처음이면 편집기 선택: nano
```
맨 아래에 한 줄 추가:
```
0 4 * * * /bin/bash /home/ubuntu/nfcreview/deploy/backup.sh >> /home/ubuntu/nfcreview/deploy/backup.log 2>&1
```

**백업은 복구해봐야 백업이에요.** 한 달에 한 번, 임시 DB에 복구해서 데이터가 들어 있는지 확인해요:
```bash
docker compose exec -T db createdb -U nfcreview restore_test
gunzip -c backups/백업파일.sql.gz | docker compose exec -T db psql -q -U nfcreview -d restore_test
docker compose exec -T db psql -U nfcreview -d restore_test -c "SELECT count(*) FROM card_event;"
docker compose exec -T db dropdb -U nfcreview restore_test
```

**서버 밖에도 보관하기**: 서버가 통째로 날아가면 서버 안의 백업도 같이 날아가요. 가끔 PC로 받아두세요 (PC cmd):
```
scp -i nfc-key.pem ubuntu@탄력적IP:~/nfcreview/deploy/backups/백업파일.sql.gz .
```

## 4. 장애 대응 순서

알림이 오면 이 순서대로:
1. 폰으로 `https://내도메인/actuator/health` 직접 열어보기
2. ssh 접속 → `cd ~/nfcreview/deploy && docker compose ps` (Up인지, Restarting인지)
3. `docker compose logs --tail 200 app`으로 에러 확인
4. 흔한 원인 확인
   - 디스크: `df -h` (사용률이 90%를 넘으면 `docker image prune -a`로 안 쓰는 이미지 정리, 오래된 백업 정리)
   - 메모리: `free -h`
   - 방금 배포했다면 → 10부의 롤백
5. 일단 살리기: `docker compose restart app` (DB 문제면 `docker compose restart`)
6. 살린 뒤 **원인과 조치를 메모**해두기. 같은 일이 두 번째로 생기면 5분 만에 끝나요.

## 5. 정기 점검

- 매주: 통계 훑어보기 (갑자기 0이면 카드나 서버 문제일 수 있어요), 로그의 ERROR 확인
- 매월
  - 서버 업데이트: `sudo apt update && sudo apt upgrade -y` (재부팅이 필요하다고 나오면 영업 끝난 뒤 `sudo reboot`)
  - 이미지 업데이트: `docker compose pull && docker compose up -d` (PostgreSQL 17, Caddy 2의 보안 패치)
  - 디스크 확인, 백업 파일 PC로 받기, 복구 연습
- 매년: 도메인 연장 확인

## 6. 빨간주막 실제 카드 교체

1. **영업이 끝난 뒤** 진행해요
2. `https://내도메인/admin`에서 가게 정보와 혜택 문구 확인 (예: 리뷰를 남겨주시면 시원한 음료나 귀여운 디저트를 드려요)
3. 테이블 수만큼 카드 발급. 위치 메모는 "1번 테이블", "2번 테이블"… (카드별 통계가 곧 테이블별 통계가 돼요)
4. 폰의 **NFC Tools** → 쓰기 → 레코드 추가 → URL → 관리 화면에서 복사한 주소 → 카드에 대고 쓰기
5. 카드를 태그해서 랜딩이 뜨는지, 관리 화면 통계에 TAP이 올라가는지 확인
6. **쓰기 잠금**: 잠그지 않은 NFC 카드는 손님 누구나 NFC Tools로 주소를 바꿔 쓸 수 있어요. NFC Tools의 기타(Other) 메뉴에 있는 **비밀번호 설정**으로 쓰기를 막아두세요. 영구 잠금(Lock)은 되돌릴 수 없어서, 나중에 주소를 바꿀 가능성을 생각하면 비밀번호 방식이 나아요. 도메인과 코드가 안 바뀌게 설계했으니 다시 쓸 일은 거의 없을 거예요.
7. 기존 GitHub Pages 랜딩 페이지는 한동안 그대로 두세요. 혹시 옛 주소가 남은 카드가 있어도 손님이 막히지 않게요.

## 7. 다음 단계 (포트폴리오 확장 아이디어)

- 사장님별 계정과 권한: 다른 가게에 팔 때 사장님이 자기 가게 통계만 보게
- 시간대별 통계: 몇 시에 태그가 많은지
- 무중단 배포: 새 컨테이너를 먼저 띄우고 준비되면 교체
- 에러가 나면 텔레그램이나 슬랙으로 알림
- 부하 테스트(k6): 서버가 초당 몇 번의 태그를 버티는지 재서 README에 기록

## 면접에서 말할 거리

README의 "핵심 설계 결정"을 자기 말로 설명할 수 있으면 돼요. 특히:
- 기록 실패와 손님 이동을 분리한 이유, DB 장애 때 마지막 정보로 대체한 방식과 그 한계
- UTC 저장 + 한국 날짜 집계
- Flyway + validate로 스키마를 코드로 관리한 경험
- 테스트를 통과해야만 배포되는 파이프라인과 SHA 태그 롤백
- 실제 매장에서 운영하며 겪은 문제와 대응 (4번의 운영 메모가 여기서 빛나요)
