# 6부. 통계

> 목표: 가게별로 기간(7/30/90일) 동안의 태그 수, 리뷰 버튼 수, 전환율을 날짜별·카드별로 보여준다.

## 먼저 알아둘 개념

**세는 일은 DB에게**: 기록이 10만 건이면 10만 줄을 자바로 가져와서 세는 대신, DB에 `GROUP BY`로 세게 해서 결과 몇십 줄만 받아요. 네트워크도 메모리도 아껴요.

**한국 날짜로 묶기**: 저장은 UTC지만 사장님은 "한국 날짜 기준 어제 몇 명"이 궁금해요. `occurred_at AT TIME ZONE 'Asia/Seoul'`로 한국 시각으로 바꾼 뒤 날짜로 묶어요. 기간의 시작점도 "한국 시간 자정"으로 계산해요(`StatsService`).

**빈 날짜 채우기**: 태그가 0건인 날은 DB 결과에 아예 없어요. 그대로 표를 그리면 그날이 사라져서 추세를 잘못 읽게 되니, 자바에서 기간의 모든 날짜를 만들고 없는 날은 0으로 채워요.

**`LEFT JOIN` + `FILTER`**: 카드별 통계에는 태그가 한 번도 없는 카드도 나와야 해요. 그래서 카드를 기준으로 기록을 LEFT JOIN 하고, `COUNT(...) FILTER (WHERE event_type = 'TAP')`로 종류별로 따로 세요.

**네이티브 쿼리 + 인터페이스 프로젝션**: 시간대 변환처럼 PostgreSQL 전용 문법이 필요해서 JPQL 대신 SQL을 직접 써요. 결과는 인터페이스(`DailyCountRow`)로 받는데, SQL의 별칭(`AS day`)과 getter 이름(`getDay()`)이 맞아야 연결돼요. PostgreSQL은 별칭을 소문자로 바꾸니까 별칭을 한 단어 소문자로 지었어요.

**조회 전용 저장소 분리**: 저장용(`CardEventRepository`)과 통계용(`CardEventStatsRepository`)을 나눴어요. 통계용은 `Repository`만 상속해서 save·delete 같은 메서드가 아예 없어요. "읽기만 하는 곳"이 코드 구조로 드러나요.

**전환율의 한계**: 전환율 = 리뷰 버튼 수 ÷ 태그 수. 실제로 리뷰를 썼는지는 네이버만 알아요. 그래도 카드 문구나 혜택을 바꿨을 때 효과를 비교하기엔 충분히 좋은 숫자예요.

## 할 일

1. `application.yml`에 `(6부)` 부분(`spring.web.locale`, `spring.web.locale-resolver`) 추가. 날짜 옆 요일(`E`)은 화면 언어를 따라가는데, 이게 없으면 영어로 설정된 폰이나 curl에서 `09.24 (Thu)`처럼 나와요. 한국 가게용 서비스라 한국어로 고정해요.
2. **이번에 만들 파일**
```
src/main/java/.../repository/CardEventStatsRepository.java
src/main/java/.../service/StatsService.java
src/main/java/.../controller/AdminStatsController.java
src/main/resources/templates/admin/stats.html
```

## 데이터 만들기 (cmd)

```
for /L %i in (1,1,20) do @curl -s -o NUL http://localhost:8080/t/test01
for /L %i in (1,1,7) do @curl -s -o NUL http://localhost:8080/go/test01
```
태그 20번, 버튼 7번이에요. (배치 파일 `.bat` 안에 적을 때는 `%i` 대신 `%%i`)

## 확인하기

1. 가게 목록 → "통계" → 오늘 날짜에 태그와 버튼 숫자가 올라가 있는지 (앞 파트에서 누른 것까지 합쳐져요)
2. 최근 7일 / 30일 / 90일 전환
3. 카드별 표에 새로 발급한(태그 0) 카드도 0으로 보이는지
4. 날짜 옆 요일이 `(목)`처럼 한국어로 나오는지
5. **도전**: `CardEventStatsRepository`의 SQL을 pgAdmin에 붙여넣고 `:storeId`는 `1`, `:since`는 `'2026-09-01 00:00:00+09'`처럼 실제 값으로 바꿔 실행해보기
6. **도전**: SQL 앞에 `EXPLAIN ANALYZE`를 붙여 실행 계획 보기. 데이터가 적으면 인덱스를 안 쓸 수도 있어요(작은 표는 통째로 읽는 게 더 빨라서). 수만 건이 쌓이면 달라져요.

## 체크포인트

- 날짜별로 묶기 전에 `AT TIME ZONE 'Asia/Seoul'`을 빼먹으면 몇 시부터 몇 시 사이의 태그가 전날로 들어갈까?
- 카드별 통계에서 `LEFT JOIN`을 그냥 `JOIN`으로 바꾸면?
- 전환율이 100%를 넘을 수도 있을까? 언제?
