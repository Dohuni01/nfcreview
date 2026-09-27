# 2부. 태그·클릭 기록과 리다이렉트

> 목표: `/t/{code}`로 들어오면 TAP을 기록하고 랜딩 페이지로, `/go/{code}`로 들어오면 CLICK을 기록하고 네이버 리뷰로 보낸다. DB가 흔들려도 손님은 막히지 않게 만든다.

## 먼저 알아둘 개념

**리다이렉트 = 302 + Location 헤더.** 서버가 "지금은 저 주소로 가세요"라고 답하면 브라우저가 자동으로 이동해요. 스프링에서는 컨트롤러가 `"redirect:주소"`를 돌려주면 돼요. 301(영구 이동)을 안 쓰는 이유: 브라우저가 기억해버려서 같은 폰의 다음 태그부터 우리 서버를 건너뛸 수 있어요. 통계가 빠지고, 주소도 못 바꿔요.

**왜 `/t` → `/l`로 한 번 더 보내나요?** 기록하는 주소(`/t`)와 보여주는 주소(`/l`)를 나눈 거예요. 손님이 랜딩 페이지에서 새로고침해도 `/l`만 다시 열리니까 태그가 두 번 세지지 않아요.

**손님 경로 3원칙** (`TapService` 맨 위 주석)
1. 기록이 실패해도 손님은 무조건 다음 페이지로 간다 → 기록을 try-catch로 감싸요.
2. DB가 잠깐 죽어도 이동은 된다 → 최근에 성공한 조회 결과를 메모리(`lastKnown`)에 기억해뒀다가 써요.
3. DB가 대답이 없으면 오래 기다리지 않는다 → `hikari.connection-timeout: 3000`. 기본값 30초였다면 손님은 30초 동안 흰 화면을 봐요.

**`@Transactional`과 "다른 빈" 규칙.** 스프링은 `@Transactional`이 붙은 빈을 대리 객체(프록시)로 감싸서, 바깥에서 호출될 때 트랜잭션을 시작해요. 그래서 **같은 클래스 안에서** `this.record()`처럼 부르면 프록시를 안 거쳐서 트랜잭션이 안 걸려요. 기록 저장을 `CardEventRecorder`라는 별도 클래스로 뺀 이유예요.

**`join fetch`**: 카드를 찾을 때 가게 정보까지 쿼리 한 번에 가져와요. `open-in-view: false`라서 조회가 끝나면 DB 연결이 닫혀요. 미리 가져오지 않으면 나중에 `card.getStore().getName()`에서 `LazyInitializationException`이 나요.

**`CardTarget`(스냅샷)**: 엔티티를 컨트롤러·화면·메모리에 그대로 넘기지 않고, 필요한 값만 담은 불변 record로 바꿔요. 엔티티는 DB 연결과 얽혀 있어서 오래 들고 있거나 여러 요청이 나눠 쓰기에 위험해요.

**`getReferenceById`**: 기록을 저장할 때 카드 전체를 SELECT하지 않고 id만 든 대리 객체를 써요. 쿼리 하나가 줄어요.

**봇 필터**: 누가 카드 주소를 카톡에 붙이면 카톡 서버가 미리보기를 만들려고 접속해요. 이런 "사람 아닌 방문"은 User-Agent로 걸러서 기록하지 않아요. 네이버 검색 로봇(Yeti)도 걸러요. 반대로 `naver`, `kakaotalk` 같은 단어로 거르면 안 돼요. 네이버·카톡 앱 안에서 링크를 연 **진짜 손님**의 User-Agent에도 `NAVER(inapp; ...)`, `KAKAOTALK` 같은 글자가 들어 있거든요.

**리다이렉트 주소 안전 처리(`RedirectUrls`)**: 리뷰 주소는 사장님이 붙여넣은 글자 그대로라서 함정이 두 개 있어요.
1. HTTP 헤더에는 한글을 그대로 못 넣어요. `...?query=빨간주막`처럼 한글이 든 주소면 Tomcat이 Location 헤더를 통째로 지워버려서, 손님은 302만 받고 빈 화면에 멈춰요. CLICK은 정상 기록되니까 통계로는 알아챌 수도 없어요.
2. `"redirect:주소"`는 `{이름}`을 URI 템플릿 변수로 해석해요. 주소에 `{foo}`가 있으면 500 에러, `{code}`가 있으면 카드 코드로 몰래 바뀌어요.

그래서 보내기 직전에 주소에 쓸 수 없는 글자(한글, 공백, 중괄호...)만 `%EB%B9%A8`처럼 퍼센트 인코딩해요. 이미 인코딩된 `%XX`는 그대로 둬서 두 번 인코딩되지 않아요. 브라우저 주소창이 하는 일과 같아요.

## 할 일

1. `application.yml`에 `spring.datasource.hikari.connection-timeout: 3000`이 있는지 확인 (1부에 넣었다면 OK)
2. **이번에 만들 파일** (앞부분 `src/main/java/com/dohun/nfcreview/` 생략)
   ```
   repository/CardRepository.java        (5부에서 쓸 메서드 2개도 미리 들어 있어요)
   repository/CardEventRepository.java   (7부에서 쓸 메서드도 미리 들어 있어요)
   service/NotFoundException.java
   service/CardTarget.java
   service/CardEventRecorder.java
   service/TapService.java
   controller/TapController.java
   controller/RedirectUrls.java          (리다이렉트 주소 안전 처리)
   ```
3. 앱을 실행하고 pgAdmin에서 테스트 데이터를 넣어요. 리뷰 주소는 지금 랜딩 페이지 버튼에 걸어둔 네이버 리뷰 주소를 쓰세요.
   ```sql
   INSERT INTO store (name, review_url, benefit_text)
   VALUES ('빨간주막', '여기에_네이버_리뷰_주소', '리뷰를 남겨주시면 시원한 음료를 드려요');

   INSERT INTO card (code, store_id, label)
   VALUES ('test01', (SELECT id FROM store WHERE name = '빨간주막'), '테스트 카드');
   ```

## 코드 읽기 포인트

- `TapController`는 HTTP만 담당해요: 주소에서 값 꺼내기, 서비스 부르기, 리다이렉트할지 화면을 그릴지 고르기. 규칙은 전부 `TapService`에 있어요.
- `TapService.find()`의 `catch`가 원칙 2, `recordSafely()`의 `catch`가 원칙 1이에요.
- `findAndRecord()`: DB 조회가 방금 실패했다면(`fromDatabase == false`) 기록 시도 자체를 건너뛰어요. 어차피 실패할 걸 또 3초 기다릴 필요가 없으니까요.
- 없는 카드면 `NotFoundException` → `@ResponseStatus(NOT_FOUND)` 덕분에 404 응답이 돼요.
- `/go`는 `"redirect:" + RedirectUrls.headerSafe(리뷰주소)`예요. `/t`의 `"redirect:/l/" + 코드`는 그대로 둬요. 코드는 우리가 만든 8자리 영문·숫자라서 위험한 글자가 없거든요.

## 확인하기 (cmd)

```
curl -i http://localhost:8080/t/test01
```
`HTTP/1.1 302`와 `Location: /l/test01`이 보이면 성공이에요.

```
curl -i http://localhost:8080/go/test01
```
`Location:`에 네이버 리뷰 주소가 보이면 성공. pgAdmin에서:
```sql
SELECT id, event_type, occurred_at, user_agent FROM card_event ORDER BY id;
```
TAP 한 줄, CLICK 한 줄이 있어야 해요.

```
curl -i -A "kakaotalk-scrap/1.0" http://localhost:8080/t/test01
```
302는 나오지만 card_event는 안 늘어나요 (봇 필터).

```
curl -i http://localhost:8080/t/nope
```
404가 나오고 콘솔에 `WARN ... 알 수 없는 카드: code=nope`가 찍혀요. curl로 열면 본문이 JSON(`{"status":404,...}`)으로 나오는데 정상이에요. curl은 "아무 형식이나 괜찮아"라고 요청하고, 브라우저는 "HTML을 줘"라고 요청해서 그래요. 폰 브라우저에서는 3부에서 만들 안내 화면이 떠요.

**한글 주소 실험**: pgAdmin에서 리뷰 주소를 잠깐 한글이 든 주소로 바꿔봐요.
```sql
UPDATE store SET review_url = 'https://m.search.naver.com/search.naver?query=빨간주막' WHERE name = '빨간주막';
```
```
curl -i http://localhost:8080/go/test01
```
`Location: ...query=%EB%B9%A8%EA%B0%84%EC%A3%BC%EB%A7%89`처럼 인코딩된 주소가 보이면 성공이에요. 확인했으면 원래 리뷰 주소로 되돌려두세요.

`/l/test01`은 아직 에러가 나는 게 정상이에요. 화면은 3부에서 만들어요.

## 장애 실험 (꼭 해보세요)

1. `curl -i http://localhost:8080/t/test01`을 한 번 성공시켜요 (서버가 이 카드를 기억하게)
2. **관리자 권한** cmd에서 PostgreSQL을 꺼요. 서비스 이름은 `services.msc`에서 "postgresql"로 찾아요.
   ```
   net stop postgresql-x64-17
   ```
3. 다시 `curl -i http://localhost:8080/t/test01` → 약 3초 뒤 **여전히 302**. 콘솔에는 `ERROR ... 카드 조회 실패 → 기억해둔 정보 사용=true`
4. `net start postgresql-x64-17`로 다시 켜기

DB가 죽었는데도 손님은 랜딩으로 갔어요. 대신 그동안의 태그는 기록되지 않았죠. "무엇을 포기하고 무엇을 지킬지" 정하는 게 운영 설계예요.

## 체크포인트

- 302 대신 301을 쓰면 무엇이 망가질까?
- `CardEventRecorder`를 따로 빼지 않고 `TapService` 안에 `@Transactional` 메서드로 두면 어떻게 될까?
- 앱을 켠 뒤 한 번도 태그된 적 없는 카드는 DB 장애 중에 어떻게 될까?
- `RedirectUrls` 없이 한글이 든 리뷰 주소로 보내면 손님 화면에 무슨 일이 생길까? 그리고 왜 통계만 봐서는 알 수 없을까?
