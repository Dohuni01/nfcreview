# 3부. 랜딩 페이지

> 목표: `/l/{code}`에서 가게 이름·혜택 문구·리뷰 버튼이 있는 모바일 페이지를 보여주고, 없는 카드나 서버 오류 때도 손님이 알아볼 수 있는 화면을 띄운다.

## 먼저 알아둘 개념

**서버 사이드 렌더링(Thymeleaf)**: 서버가 HTML에 값을 채워 완성된 페이지를 보내요. 손님 폰은 받은 HTML을 그리기만 하면 되니까 가볍고 빨라요.

**`th:text`는 자동 이스케이프**: 혜택 문구에 누가 `<script>...</script>`를 적어도 실행되지 않고 글자 그대로 보여요. XSS(남의 페이지에 스크립트 심기) 방어의 기본이에요. `th:utext`(이스케이프 안 함)는 이유 없이 쓰지 마세요.

**`@{...}` 링크 표현식**: `@{/go/{code}(code=${target.code})}` → `/go/test01`. 앱이 다른 경로 아래에 배포돼도 주소가 알아서 맞춰져요.

**에러 페이지 규칙**: `templates/error/` 폴더에 `404.html`, `4xx.html`, `5xx.html`을 두면 스프링이 에러 종류에 맞춰 알아서 보여줘요. 정확한 번호(`404.html`)를 먼저 찾고, 없으면 묶음(`4xx.html`, `5xx.html`)을 찾아요. 하나도 없으면 영어로 된 "Whitelabel Error Page"가 떠요. `4xx.html`은 4부 이후 관리 화면에서 자주 보게 돼요. 화면을 오래 열어둬서 로그인이 풀린 채로 저장을 누르면 CSRF 토큰이 맞지 않아 403이 나거든요.

**모바일 우선**: `<meta name="viewport" ...>`가 없으면 폰에서 PC 화면을 축소한 것처럼 글씨가 깨알같이 나와요.

## 할 일

1. `build.gradle`에 `Part 3` 줄 추가 → IntelliJ 오른쪽 Gradle 창의 새로고침(코끼리 아이콘)
   ```groovy
   implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
   ```
2. `application.yml`에 `(3부)` 부분 추가: `spring.thymeleaf.cache: false`
3. **이번에 만들 파일**
   ```
   src/main/resources/templates/landing.html
   src/main/resources/templates/error/404.html
   src/main/resources/templates/error/4xx.html
   src/main/resources/templates/error/5xx.html
   src/main/resources/static/css/landing.css
   ```

디자인은 카드 시안(한지 질감, 붉은 톤, 구리색 원판 NFC 아이콘)에 맞췄어요. 지금 GitHub Pages 랜딩에 쓰던 HTML/CSS가 있다면 그걸 옮겨와도 돼요. `th:text`, `th:href`가 붙은 자리만 살려두면 돼요.

## 확인하기

1. 브라우저로 http://localhost:8080/t/test01 → 주소창이 `/l/test01`로 바뀌면서 랜딩 페이지
2. 리뷰 버튼 → 네이버 리뷰 페이지
3. pgAdmin: TAP 1, CLICK 1이 늘었는지
4. 랜딩에서 새로고침(F5) → TAP이 **안** 늘어나는지 (2부에서 `/t`와 `/l`을 나눈 효과)
5. http://localhost:8080/t/nope → 404 안내 화면
6. 크롬 개발자도구(F12) → 폰 모양 아이콘으로 모바일 화면 확인
7. `curl -i -X POST -H "Accept: text/html" http://localhost:8080/t/test01` → `405`와 "요청을 처리하지 못했어요" (4xx.html). 4부에서 로그인을 붙인 뒤에는 CSRF 검사가 먼저 막아서 `403`이 나와요.

## 내 폰으로 확인하기

1. PC와 폰을 같은 와이파이에 연결
2. cmd에서 `ipconfig` → "IPv4 주소" (예: 192.168.0.12)
3. 폰 브라우저로 `http://192.168.0.12:8080/t/test01`
4. 안 열리면 Windows 방화벽 문제예요. 앱을 처음 실행할 때 뜨는 방화벽 창에서 "허용"을 눌러야 해요. 놓쳤다면 "Windows Defender 방화벽 → 앱 허용"에서 Java(OpenJDK)를 허용하세요.
5. 여분 NFC 태그가 있다면 NFC Tools로 이 주소를 써서 진짜로 태그해봐요. (실제 매장 카드는 11부에서 운영 주소로 써요)

## 체크포인트

- `th:text` 대신 `th:utext`로 혜택 문구를 출력하면 어떤 위험이 있을까?
- 랜딩 페이지를 `/t/{code}`에서 바로 보여주지 않고 리다이렉트를 한 번 더 하는 이유는?
