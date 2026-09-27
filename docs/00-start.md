# NFC 리뷰 카드 서버 · 따라 만들기 가이드

손님이 테이블의 NFC 카드를 태그하면 우리 서버가 기록하고, 혜택 안내 페이지를 보여주고, 리뷰 버튼을 누르면 네이버 리뷰로 보내는 서비스예요. 코드 작성부터 배포·운영까지 전부 직접 해봐요.

## 이 폴더를 쓰는 방법

- 이 폴더 전체가 **완성본**이에요. 모든 파일에 "왜 이렇게 짰는지" 주석을 달아뒀어요.
- `docs/`의 가이드는 완성본을 **11개 파트로 나눈 순서표**예요. 파트마다 "이번에 만들 파일"이 나오면, 완성본의 같은 경로 파일을 옆에 띄워두고 **직접 타이핑**하세요. 복사해서 붙이면 빨리 끝나지만 머리에 남지 않아요.
- 파트 끝의 **확인하기**가 전부 되면 다음 파트로 넘어가요. 막히면 에러 메시지를 그대로 Claude에게 붙여넣으세요.
- `build.gradle`과 `application.yml`은 파트를 거치며 조금씩 늘어나요. 완성본 파일에 `Part N`, `(N부)` 표시를 해뒀으니 해당 파트의 줄만 추가하면 돼요.
- 파트가 끝날 때마다 커밋하세요. 예: `git commit -m "2부: 태그 기록과 리다이렉트"`. 커밋 기록 자체가 포트폴리오의 성장 기록이 돼요.
- 앞에서 맛보기로 만든 1단계 코드(`target_url`로 GitHub Pages에 보내던 버전)는 이 가이드로 대체돼요.

## 전체 흐름

```
손님 폰 ──태그──▶ GET /t/{code}   TAP 기록 → 302 → /l/{code}
                 GET /l/{code}   랜딩 페이지 (가게 이름, 혜택 문구, 리뷰 버튼)
        ──버튼──▶ GET /go/{code}  CLICK 기록 → 302 → 네이버 리뷰 페이지

관리자(나) ─────▶ /admin          로그인 → 가게·카드 관리, 날짜별·카드별 통계
```

## 파트 목록

| 파트 | 파일 | 내용 | 끝나면 |
|---|---|---|---|
| 1 | 01-project-and-db.md | 프로젝트 생성, DB 설계(Flyway), 엔티티 | 앱이 켜지고 테이블이 생김 |
| 2 | 02-tap-and-redirect.md | 태그·클릭 기록, 리다이렉트, 장애 대비 | curl로 302 확인 |
| 3 | 03-landing-page.md | 랜딩 페이지, 에러 페이지 | 폰에서 태그 → 랜딩 → 네이버 |
| 4 | 04-admin-login.md | 관리자 로그인 (Spring Security) | /admin이 잠기고 로그인됨 |
| 5 | 05-admin-stores-cards.md | 가게·카드 관리 화면 | 화면에서 카드 발급 |
| 6 | 06-stats.md | 통계 | 날짜별·카드별 숫자 |
| 7 | 07-tests.md | 단위·통합 테스트 | gradlew test 통과 |
| 8 | 08-docker.md | Docker로 포장 | docker compose up으로 실행 |
| 9 | 09-deploy-server.md | 클라우드 서버, 도메인, HTTPS | https://내도메인 접속 |
| 10 | 10-auto-deploy.md | GitHub Actions 자동 배포 | push하면 서버에 반영 |
| 11 | 11-operations.md | 모니터링, 백업, 장애 대응, 실제 카드 교체 | 빨간주막 카드 교체 |

## 완성본 폴더 구조

```
nfcreview/
├─ build.gradle, settings.gradle, gradlew   빌드 설정
├─ Dockerfile, .dockerignore, compose.yaml  도커 (8부)
├─ deploy/                                  운영 서버용 파일 (9부~)
├─ .github/workflows/deploy.yml             자동 배포 (10부)
├─ docs/                                    이 가이드
└─ src/
   ├─ main/java/com/dohun/nfcreview/
   │  ├─ domain/       엔티티: Store, Card, CardEvent, EventType
   │  ├─ repository/   DB 접근
   │  ├─ service/      규칙: TapService(손님 흐름), AdminService, StatsService ...
   │  ├─ controller/   주소와 화면 연결
   │  └─ config/       보안, 설정값
   ├─ main/resources/
   │  ├─ application.yml, application-prod.yml
   │  ├─ db/migration/V1__init.sql
   │  ├─ templates/    HTML (Thymeleaf)
   │  └─ static/css/
   └─ test/java/...    테스트 (7부)
```

## 준비물

- JDK 21 (cmd에서 `java -version`으로 확인)
- IntelliJ IDEA, Git, 로컬 PostgreSQL + pgAdmin
- 8부부터 Docker Desktop, 9부부터 AWS 계정과 도메인
- 명령어는 전부 Windows **cmd** 기준이에요. 서버(리눅스)에서 치는 명령은 따로 표시해뒀어요.

## 완성본을 먼저 돌려보고 싶다면

1. pgAdmin에서 `CREATE DATABASE nfcreview;`
2. `src/main/resources/application.yml`의 `${DB_PASSWORD:1234}`에서 1234를 내 PostgreSQL 비밀번호로 바꾸기
3. IntelliJ에서 이 폴더 열기 → Gradle 로딩 기다리기 → `NfcreviewApplication` 실행
4. http://localhost:8080/admin → `admin` / `admin1234`로 로그인 → 가게 추가 → 카드 발급 → 카드 주소로 접속

## 기술 스택

Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate 7) · Flyway · PostgreSQL 17 · Thymeleaf · Spring Security · Docker · Caddy · GitHub Actions
