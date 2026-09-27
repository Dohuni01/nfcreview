# 4부. 관리자 로그인 (Spring Security)

> 목표: `/admin`으로 시작하는 주소는 로그인해야 들어갈 수 있고, 손님 주소(`/t`, `/l`, `/go`)는 지금처럼 누구나 쓸 수 있게 한다.

## 먼저 알아둘 개념

**인증 vs 인가**: 인증은 "너 누구야?"(로그인), 인가는 "너 이거 해도 돼?"(권한)예요. `SecurityConfig`의 `authorizeHttpRequests`가 인가 규칙이에요.

**필터 체인**: 스프링 시큐리티는 요청이 컨트롤러에 닿기 전에 거치는 검문소 줄이에요. 로그인 안 한 사람이 `/admin/stores`로 오면 컨트롤러까지 가지도 못하고 로그인 화면으로 돌려보내져요.

**규칙은 위에서부터**: `/admin/login`을 `permitAll`로 먼저 적고, 그다음 `/admin/**`를 막아요. 순서를 바꾸면 로그인 화면까지 막혀서 영원히 로그인을 못 해요.

**비밀번호 해시(BCrypt)**: 비밀번호를 되돌릴 수 없는 모양(`{bcrypt}$2a$10$...`)으로 바꿔 보관하고, 로그인할 때 입력값을 같은 방식으로 바꿔 비교해요. 메모리나 DB가 털려도 원래 비밀번호는 알 수 없게요.

**CSRF 보호**: 내가 관리자 페이지에 로그인한 채로 이상한 사이트에 들어갔을 때, 그 사이트가 몰래 "카드 끄기" 요청을 보내는 공격을 막아요. 스프링 시큐리티가 폼마다 비밀 토큰을 요구하고, Thymeleaf의 `th:action`이 그 토큰을 폼에 자동으로 넣어줘요. 그래서 로그아웃도 링크가 아니라 POST 폼이에요.

**설정값을 객체로(`@ConfigurationProperties`) + 검증(`@Validated`)**: `app.admin.password` 같은 값을 `AppProperties` record로 받고, 비어 있거나 8자 미만이면 앱이 아예 안 켜지게 해요.

**관리자 계정을 DB가 아니라 설정값으로 둔 이유**: 관리자는 나 한 명이라 가장 단순한 방법을 골랐어요. 나중에 사장님별 계정이 필요해지면 그때 DB 테이블로 옮기면 돼요(11부 "다음 단계").

## 할 일

1. `build.gradle`에 `Part 4` 줄 추가 → Gradle 새로고침
   ```groovy
   implementation 'org.springframework.boot:spring-boot-starter-security'
   implementation 'org.springframework.boot:spring-boot-starter-validation'
   ```
   ⚠️ security를 넣는 순간 **모든 주소가 잠겨요**(스프링 기본값). 아래 `SecurityConfig`를 만들어야 손님 주소가 다시 열려요.
2. `application.yml`에 `(4부)` 부분(`app:` 블록) 추가
3. `NfcreviewApplication`에 `@ConfigurationPropertiesScan` 한 줄 추가
4. **이번에 만들 파일**
   ```
   src/main/java/.../config/AppProperties.java
   src/main/java/.../config/SecurityConfig.java
   src/main/java/.../controller/AdminLoginController.java
   src/main/resources/templates/admin/fragments.html
   src/main/resources/templates/admin/login.html
   src/main/resources/static/css/admin.css
   ```

## 코드 읽기 포인트

- `fragments.html`은 여러 관리 화면이 같이 쓰는 조각(머리말, 상단 메뉴, 알림)이에요. `th:replace="~{admin/fragments :: nav}"`로 가져다 써요. 같은 코드를 화면마다 복사하지 않으려고요.
- `login.html`의 `${param.error}`는 주소 끝에 `?error`가 있는지 봐요. 로그인이 실패하면 스프링 시큐리티가 `/admin/login?error`로 보내줘요.
- `AppProperties`의 생성자에서 주소 끝의 `/`를 잘라요. `https://도메인/`로 적어도 `https://도메인//t/...`처럼 되지 않게요.

## 확인하기

1. http://localhost:8080/admin/stores → 로그인 화면으로 튕김
2. 틀린 비밀번호 → "아이디 또는 비밀번호가 맞지 않아요"
3. `admin` / `admin1234` → 로그인 성공 → **404 화면이 정상**이에요. 가게 목록 화면은 5부에서 만들어요.
4. 로그인 없이 http://localhost:8080/t/test01 → 여전히 랜딩으로 잘 가요
5. **실험**: application.yml의 관리자 비밀번호 기본값을 `1234`(4자)로 바꾸고 실행 → 검증 실패로 앱이 안 켜져요 → 되돌리기

## 체크포인트

- 규칙 순서를 `/admin/**` 먼저, `/admin/login` 나중으로 바꾸면?
- 로그아웃을 `<a href="/admin/logout">` 링크로 만들면 왜 안 될까?
- 비밀번호를 해시하지 않고 그대로 보관하면 어떤 일이 생길 수 있을까?
