# 9부. 서버 배포 + 도메인 + HTTPS

> 목표: 인터넷 어디서나 `https://내도메인`으로 접속되게 한다.

## 완성 구조

```
손님 폰 ──https──▶ [Caddy :443]  HTTPS 인증서 자동 발급·갱신
                        │ http (서버 내부)
                        ▼
                   [app :8080]   밖에서는 직접 못 들어와요
                        │
                        ▼
                   [db :5432]    밖에서는 직접 못 들어와요
```

## 먼저 알아둘 개념

**리버스 프록시(Caddy)**: 손님의 요청을 대신 받아 앱에 넘겨주는 문지기예요. HTTPS 암호화를 Caddy가 전부 처리하고, 도메인만 적어두면 Let's Encrypt 인증서를 자동으로 받고 갱신까지 해줘요. 앱 포트(8080)는 밖에 열지 않아서 반드시 Caddy를 거치게 돼요.

**forward headers**: Caddy → 앱 구간은 서버 안쪽이라 http예요. 그대로면 앱은 "http로 요청이 왔네"라고 착각해서, 로그인 뒤 리다이렉트를 http 주소로 보내는 식의 문제가 생겨요. Caddy가 붙여주는 `X-Forwarded-Proto: https` 헤더를 믿으라고 `server.forward-headers-strategy: native`를 켜요.

**prod 프로필**: `SPRING_PROFILES_ACTIVE=prod`면 `application-prod.yml`이 덧씌워져요. 여기엔 **기본값이 없어요**. 환경변수를 빠뜨리면 앱이 아예 안 켜지는데, 그게 틀린 값으로 조용히 켜지는 것보다 안전해요. 로그인 쿠키도 https에서만 보내게(`secure: true`) 해요.

**`${이름:}`의 콜론이 중요해요**: prod.yml은 `password: ${ADMIN_PASSWORD:}`처럼 콜론 뒤를 비워뒀어요. "환경변수가 없으면 빈 값"이라는 뜻이고, 빈 값은 `AppProperties`의 `@NotBlank`에 걸려서 앱이 안 켜져요. 콜론 없이 `${ADMIN_PASSWORD}`라고만 쓰면, 스프링은 못 찾은 이름을 **글자 그대로** 넣고 켜져버려요. 그러면 관리자 비밀번호가 `${ADMIN_PASSWORD}`라는 글자가 되고, 공개 저장소를 본 사람 누구나 로그인할 수 있어요. (실제로 확인하고 고친 문제예요)

**헬스 체크(Actuator)**: `/actuator/health`가 `{"status":"UP"}`을 돌려주면 앱과 DB가 살아 있다는 뜻이에요. 11부의 모니터링이 이 주소를 주기적으로 찔러봐요.

**`.env`**: 비밀번호 같은 비밀값은 서버의 `deploy/.env` 파일에만 두고, 절대 Git에 올리지 않아요.

## 1. 코드 준비 (PC)

1. `build.gradle`에 `Part 9` 줄 추가
   ```groovy
   implementation 'org.springframework.boot:spring-boot-starter-actuator'
   ```
2. `application.yml`에 `(9부)` 부분(`management:` 블록) 추가
3. **이번에 만들 파일**
   ```
   src/main/resources/application-prod.yml
   deploy/compose.yaml
   deploy/Caddyfile
   deploy/.env.example
   deploy/backup.sh
   ```
4. GitHub에 저장소를 만들고 push. `git status`로 `.env`, `.pem`이 목록에 **없는지** 꼭 확인하세요. 포트폴리오로 쓸 거라 Public을 추천해요(서버에서 받기도 쉬워요). 비밀값은 코드에 없으니 공개해도 괜찮아요.

## 2. 서버 만들기 (AWS EC2 기준)

- 리전: **서울 (ap-northeast-2)**. 손님 폰과 가까울수록 빨라요.
- OS: Ubuntu 24.04 LTS, 아키텍처 x86_64
- 크기: 메모리 2GB 권장 (1GB면 아래 스왑 설정이 필수)
- 키 페어: 새로 만들고 `.pem` 파일 다운로드 (다시 받을 수 없으니 잘 보관)
- 보안 그룹 인바운드: **22(SSH), 80(HTTP), 443(HTTPS)**. 22는 10부 자동 배포(GitHub 서버가 접속) 때문에 모든 곳에서 허용해야 해요. EC2 우분투는 비밀번호 로그인이 막혀 있고 키로만 들어올 수 있어서 괜찮아요.
- 탄력적 IP(Elastic IP) 할당 → 인스턴스에 연결. 서버를 껐다 켜도 IP가 안 바뀌어요.
- 요금은 가입 시점의 프리티어·크레딧 조건에 따라 달라요. 결제 알림(Billing alarm)을 꼭 켜두세요.

## 3. 접속하기 (PC의 cmd)

`.pem` 파일 권한이 너무 열려 있으면 ssh가 거부해요. 처음 한 번만:
```
icacls nfc-key.pem /inheritance:r
icacls nfc-key.pem /grant:r "%USERNAME%":"(R)"
```
접속:
```
ssh -i nfc-key.pem ubuntu@탄력적IP
```

## 4. 서버 기본 설정 (서버에서)

```bash
# 스왑 2GB: 메모리가 모자랄 때 디스크를 임시 메모리로 써요. 메모리 부족으로 앱이 죽는 걸 막는 보험.
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 도커 설치
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit
```
다시 ssh로 접속해야 `docker` 명령을 sudo 없이 쓸 수 있어요. `docker version`으로 확인하세요.

## 5. 도메인 연결

1. 가비아 같은 곳에서 도메인 구매
2. DNS 설정에서 **A 레코드** 추가: 호스트 `@`(또는 `review` 같은 서브도메인) → 값: 탄력적 IP
3. PC cmd에서 `nslookup 내도메인` → 탄력적 IP가 나오면 준비 완료 (몇 분~몇 시간 걸릴 수 있어요)

⚠️ **카드에 쓸 주소의 도메인은 한 번 정하면 계속 써야 해요.** 도메인 만료일을 캘린더에 적어두고 자동 연장을 켜세요.

## 6. 앱 띄우기 (서버에서)

```bash
git clone https://github.com/내아이디/nfcreview.git ~/nfcreview
cd ~/nfcreview
docker build -t nfcreview-app:local .      # 서버에서 직접 이미지 빌드 (몇 분)

cd deploy
cp .env.example .env
openssl rand -base64 24                    # DB 비밀번호로 쓸 랜덤 값 (복사해두기)
nano .env                                  # 값 채우기 → Ctrl+O, Enter(저장), Ctrl+X(나가기)

docker compose up -d
docker compose logs -f                     # Ctrl+C로 빠져나와요 (앱은 계속 돌아요)
```
`.env`에 채울 값:
- `DOMAIN`: 내 도메인 (https:// 없이)
- `APP_IMAGE`: `nfcreview-app:local`
- `DB_PASSWORD`: 위에서 만든 랜덤 값
- `ADMIN_USERNAME`, `ADMIN_PASSWORD`: 관리자 계정 (비밀번호 8자 이상, 로컬용 admin1234 금지)

## 확인하기

1. 폰으로 `https://내도메인/actuator/health` → `{"status":"UP", ...}`, 주소창에 자물쇠
2. `https://내도메인/admin` → 로그인 → 가게 등록, 카드 발급 (운영 DB는 새것이라 비어 있어요)
3. 관리 화면의 "NFC에 쓸 주소"(`https://내도메인/t/xxxxxxxx`)로 폰에서 접속 → 랜딩 → 네이버

## 막히면

- 인증서 발급 실패: `docker compose logs caddy`. 대부분 DNS가 아직 안 퍼졌거나 보안 그룹의 80/443이 닫혀 있어요.
- 앱이 계속 재시작: `docker compose logs app`에서 `APPLICATION FAILED TO START` 아래를 봐요.
  - `Property: app.admin.password` + `Reason: must not be blank` → `.env`에 값이 비어 있어요 (`size must be between 8 and ...`면 8자 미만)
  - `Failed to configure a DataSource: 'url' attribute is not specified` → DB_URL이 비어 있어요
  - 보통은 그 전에 `docker compose up`이 `ADMIN_PASSWORD is required in .env`라며 멈춰요. compose와 앱이 이중으로 막아주는 거예요.
- DB 접속 실패(`password authentication failed`): DB는 **처음 만들어질 때의** `DB_PASSWORD`로 굳어져요. 나중에 `.env`만 바꾸면 안 맞아요. 아직 데이터가 없다면 `docker compose down -v`로 지우고 다시 `up -d`.
- 이미지 빌드 중 멈춤, 서버 먹통: 메모리 부족이에요. `free -h`로 스왑이 켜졌는지 확인하세요.
- `docker: permission denied`: `usermod` 후 다시 접속을 안 했어요.

## 체크포인트

- 앱의 8080 포트를 밖에 직접 열지 않고 Caddy만 여는 이유는?
- `application-prod.yml`에 기본값을 일부러 안 넣은 이유는?
- `caddy-data` 볼륨을 지우면 어떤 일이 생길까?
- prod.yml에 `${ADMIN_PASSWORD:}` 대신 `${ADMIN_PASSWORD}`라고 쓰면, 환경변수를 빠뜨렸을 때 무슨 일이 생길까?
