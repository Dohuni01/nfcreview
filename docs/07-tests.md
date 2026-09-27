# 7부. 테스트

> 목표: "고친 코드가 기존 기능을 망가뜨리지 않았나"를 사람이 아니라 기계가 확인하게 한다. 10부의 자동 배포는 테스트가 통과해야만 진행돼요.

## 먼저 알아둘 개념

**단위 테스트**(`TapServiceTest`, `CardCodeGeneratorTest`): 클래스 하나의 규칙만 검사해요. DB나 스프링 없이 돌아서 1초도 안 걸려요. DB 대신 **Mockito 가짜 객체(mock)**를 넣어서 "DB가 이 카드를 돌려줬다면", "저장하다 에러가 났다면" 같은 상황을 마음대로 만들어요. 진짜 DB로는 일부러 장애를 내기 어렵지만, 가짜 객체로는 한 줄이면 돼요.

**통합 테스트**(`TapFlowIntegrationTest`, `AdminFlowIntegrationTest`): 진짜 스프링 + 진짜 DB로 "태그 → 랜딩 → 버튼" 흐름과 관리 화면·통계를 요청 단위로 확인해요. 통계 SQL(`AT TIME ZONE`, `FILTER`)은 PostgreSQL 전용 문법이라, 진짜 DB로 돌리는 테스트만 SQL 오타를 잡을 수 있어요. `MockMvc`는 서버를 띄우지 않고 가짜 HTTP 요청을 보내는 도구예요. Spring Boot 4부터는 `@AutoConfigureMockMvc`를 꼭 붙여야 쓸 수 있어요.

**`@Transactional` 롤백**: 테스트 클래스에 붙이면 테스트가 끝날 때 DB 변경을 전부 되돌려요. 로컬 DB에 테스트 데이터가 쌓이지 않아요.

**로그인·CSRF가 있는 화면 테스트**: `@WithMockUser(roles = "ADMIN")`을 붙이면 로그인 과정을 건너뛰고 "관리자로 로그인한 상태"가 돼요. POST 요청에는 `.with(csrf())`로 진짜 폼처럼 CSRF 토큰을 붙여요. 토큰을 뺀 요청이 403인지도 테스트로 확인해요.

**테스트 = 설계 문서**: 메서드 이름을 한글 문장으로 적었어요(`기록이_실패해도_이동할_곳은_돌려준다`). 2부의 "손님 경로 3원칙"이 테스트로 박제돼서, 누가(미래의 나 포함) 실수로 원칙을 깨면 빨간불이 들어와요.

**Given - When - Then**: 준비(이런 상황에서) → 실행(이걸 하면) → 검증(이렇게 돼야 한다). 테스트 코드는 대부분 이 세 덩어리예요.

## 할 일

**이번에 만들 파일** (`src/test/java/com/dohun/nfcreview/` 아래)
```
service/TapServiceTest.java
service/CardCodeGeneratorTest.java
controller/RedirectUrlsTest.java
TapFlowIntegrationTest.java
AdminFlowIntegrationTest.java
```
Initializr가 만든 `NfcreviewApplicationTests.java`는 지워도 돼요. 통합 테스트가 같은 역할(스프링이 잘 뜨는지)을 해요.

`build.gradle`의 `Part 7` 테스트 스타터가 있는지도 확인하세요. 특히 `spring-boot-starter-webmvc-test`가 있어야 `@AutoConfigureMockMvc`를 쓸 수 있어요.

## 실행하기

- IntelliJ: 테스트 클래스 옆 ▶ 버튼
- cmd (프로젝트 폴더에서):
  ```
  gradlew test
  ```
  `BUILD SUCCESSFUL`이면 통과 (테스트 23개). 결과 리포트는 `build\reports\tests\test\index.html`을 브라우저로 열어요.

통합 테스트는 진짜 DB를 쓰니까 **로컬 PostgreSQL이 켜져 있어야** 해요.

## 실험 (꼭 해보세요)

1. `TapService.recordSafely()`의 `try { ... } catch (...) { ... }`를 지우고 `recorder.record(...)` 한 줄만 남기기
2. `gradlew test` → `기록이_실패해도_이동할_곳은_돌려준다`가 빨간불
3. 되돌리고 다시 초록불

테스트가 설계 원칙을 지키고 있다는 걸 눈으로 확인하는 거예요.

하나 더: `TapController.go()`의 `RedirectUrls.headerSafe(target.reviewUrl())`를 `target.reviewUrl()`로 바꾸고 테스트 → `리뷰_주소에_한글이나_중괄호가_있어도_인코딩해서_보낸다`가 `Model has no value for key 'foo'`로 빨간불. 되돌리기.

## 막히면

- 통합 테스트만 `Connection refused` → 로컬 PostgreSQL이 꺼져 있어요
- 리눅스(WSL, 서버)에서 `Could not generate test report ... Malformed input or input contains unmappable characters` → 테스트 이름이 한글이라 생겨요. 시스템 문자 설정이 UTF-8이 아니어서 결과 파일 이름을 못 만든 거예요. `export LANG=C.UTF-8` 후 다시 실행하세요. Windows와 GitHub Actions에서는 생기지 않아요.

## 체크포인트

- 단위 테스트에서 진짜 DB 대신 가짜 객체를 쓰는 장점 두 가지는?
- 통합 테스트에서 `@Transactional`을 빼면 두 번째 실행부터 어떤 일이 생길까? (힌트: 카드 코드는 UNIQUE)
