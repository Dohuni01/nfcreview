# 8부. Docker로 포장하기

> 목표: "내 PC에선 되는데 서버에선 안 돼요"를 없앤다. 앱과 실행 환경(자바 버전 등)을 통째로 상자(이미지)에 담아서 어디서든 똑같이 돌린다.

## 먼저 알아둘 개념

**이미지 vs 컨테이너**: 이미지는 설계도(클래스), 컨테이너는 그 설계도로 띄운 실행 중인 것(객체)이에요. 이미지 하나로 컨테이너를 여러 개 띄울 수 있어요.

**멀티 스테이지 빌드(`Dockerfile`)**: 1단계에서 JDK와 Gradle로 jar를 만들고, 2단계에서는 JRE와 jar만 새 상자에 옮겨 담아요. 빌드 도구가 빠져서 이미지가 작고, 공격받을 거리도 줄어요.

**레이어 캐시**: Dockerfile은 한 줄 한 줄이 층(레이어)으로 저장돼요. `build.gradle`을 먼저 복사해서 라이브러리를 받아두면, 소스만 바꿨을 땐 라이브러리 층을 재사용해서 빌드가 훨씬 빨라요.

**root로 실행하지 않기**: 컨테이너 안에서 `spring`이라는 일반 사용자로 앱을 돌려요. 혹시 앱이 뚫려도 할 수 있는 일이 줄어요.

**컨테이너끼리의 주소**: compose로 띄운 컨테이너들은 같은 네트워크에 있고, 서로를 **서비스 이름**으로 찾아요. 앱 입장에서 DB 주소는 `localhost`가 아니라 `db`예요(`DB_URL: jdbc:postgresql://db:5432/...`). 컨테이너 안의 localhost는 "자기 자신"이거든요.

**볼륨**: 컨테이너를 지우면 안의 파일도 사라져요. DB 데이터는 볼륨(`local-db-data`)에 따로 저장해서 컨테이너를 지워도 남게 해요.

**줄바꿈(CRLF) 함정**: 윈도우는 줄 끝이 `\r\n`, 리눅스는 `\n`이에요. `gradlew` 같은 스크립트에 `\r`이 섞이면 리눅스에서 실행이 안 돼요. `.gitattributes`로 Git이 스크립트를 항상 `\n`으로 저장하게 하고, Dockerfile에서도 한 번 더 정리해요.

## 할 일

1. **Docker Desktop for Windows** 설치 (WSL 2 사용 권장) → cmd에서 `docker version`이 되면 OK
2. `build.gradle`에 `Part 8` 부분(`jar` 끄기, `bootJar` 이름을 `app.jar`로) 추가
3. **이번에 만들 파일** (프로젝트 맨 위 폴더)
   ```
   Dockerfile
   .dockerignore
   compose.yaml
   .gitattributes
   ```

## 실행하기 (cmd, 프로젝트 폴더에서)

1. IntelliJ에서 돌던 앱은 꺼요 (8080 포트가 겹쳐요)
2. 빌드하고 실행:
   ```
   docker compose up --build
   ```
   첫 빌드는 라이브러리를 받느라 몇 분 걸려요. `Started NfcreviewApplication`이 보이면 성공.
3. http://localhost:8080/admin → `admin` / `admin1234` → 가게·카드 만들고 카드 주소로 접속 (도커 DB는 새것이라 비어 있어요)
4. 끄기: `Ctrl + C` 후 `docker compose down` (DB 데이터는 볼륨에 남아요). 데이터까지 지우려면 `docker compose down -v`

## 자주 쓰는 명령

- `docker compose ps` : 지금 떠 있는 컨테이너
- `docker compose logs -f app` : 앱 로그 실시간 보기 (Ctrl+C로 빠져나와요)
- `docker compose up -d --build` : 뒤에서(백그라운드) 실행
- `docker images` : 만들어진 이미지 목록

도커 DB를 pgAdmin으로 보고 싶으면 `localhost`, 포트 **5433**, 사용자 `nfcreview`, 비밀번호 `localpass`로 접속해요. PC에 설치된 PostgreSQL(5432)과 겹치지 않게 5433으로 열어뒀어요.

## 체크포인트

- 컨테이너 안의 앱이 DB 주소로 `localhost`를 쓰면 왜 연결이 안 될까?
- 볼륨 없이 DB를 돌리다가 `docker compose down` 하면?
- 2단계 이미지에 JDK가 아니라 JRE만 넣는 이유는?
