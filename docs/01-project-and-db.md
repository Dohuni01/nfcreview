# 1부. 프로젝트와 DB 설계

> 목표: 프로젝트를 만들고, DB 테이블을 **SQL 파일로** 관리하는 방식(Flyway)을 세우고, 테이블과 짝이 되는 엔티티를 만든다.

## 먼저 알아둘 개념

**왜 `ddl-auto: update`를 안 쓰나요?**
맛보기 코드에서는 Hibernate가 엔티티를 보고 테이블을 "알아서" 고치게 했어요. 편하지만 운영에서는 위험해요. 무엇이 언제 바뀌었는지 기록이 없고, 필드 이름을 바꾸면 새 컬럼만 생기고 옛 데이터는 그대로 남는 식으로 조용히 어긋나요.

**Flyway**는 DB 변경을 `V1__init.sql`, `V2__add_xxx.sql`처럼 번호 붙은 SQL 파일로 남겨요. 앱이 켜질 때 아직 적용 안 된 파일만 순서대로 실행하고, `flyway_schema_history` 테이블에 "어디까지 적용했는지" 적어둬요. 그래서 내 PC든 운영 서버든 DB 구조가 항상 같아요. 코드처럼 Git에 이력이 남는 건 덤이에요.

**`ddl-auto: validate`**는 Hibernate가 테이블을 건드리지 않고 "엔티티와 테이블이 맞는지 검사"만 하게 해요. 어긋나면 앱이 아예 안 켜져요. 틀린 채로 조용히 돌아가는 것보다 훨씬 안전해요.

**테이블 설계**
- `store`(가게): 이름, 네이버 리뷰 주소, 혜택 문구. 리뷰 주소를 가게에 두면 주소가 바뀌어도 한 줄만 고치면 돼요.
- `card`(카드): 고유 코드(주소 끝부분), 어느 가게 카드인지, 위치 메모, 사용 여부.
- `card_event`(기록): 어떤 카드에서, 무슨 일이(TAP=태그, CLICK=리뷰 버튼), 언제, 어떤 폰으로. 태그와 클릭을 한 테이블에 `event_type`으로 구분하면 "태그 중 몇 %가 버튼을 눌렀나" 같은 비교가 쉬워요.

**인덱스**는 책 뒤의 "찾아보기"예요. 통계는 "이 카드의 이 기간 기록"을 자주 찾으니까 `(card_id, occurred_at)`에 인덱스를 걸어요. 기록이 수십만 건이 돼도 전부 뒤지지 않고 바로 찾아가요.

**시간은 `Instant` + `TIMESTAMPTZ`**: 세계 기준 시각(UTC)으로 저장해요. 클라우드 서버 시계는 보통 UTC라서, 서버 시계 그대로 저장하면 한국 새벽 1시 태그가 전날 오후 4시로 찍혀요.

**Enum은 `EnumType.STRING`**: DB에 `'TAP'`, `'CLICK'` 글자로 저장해요. 기본값(ORDINAL)은 0, 1 같은 순서 번호라서, 나중에 enum 순서만 바꿔도 과거 데이터의 뜻이 뒤바뀌어요.

## 할 일

1. **(맛보기 코드를 실행했다면)** pgAdmin에서 DB를 비워요. Flyway는 모르는 테이블이 있는 DB에서는 시작을 거부해요.
   ```sql
   DROP DATABASE nfcreview;
   CREATE DATABASE nfcreview;
   ```
   처음이라면 `CREATE DATABASE nfcreview;`만 하면 돼요.

2. **start.spring.io**에서 프로젝트 생성
   - Project: Gradle - Groovy / Language: Java / Spring Boot: 4.1.x (기본값)
   - Group: `com.dohun` / Artifact: `nfcreview` / Java: 21
   - Dependencies: **Spring Web, Spring Data JPA, Flyway Migration, PostgreSQL Driver**
   - Generate → 압축 풀고 IntelliJ로 열기 → `git init`

3. **build.gradle 확인**: 완성본 `build.gradle`의 `Part 1` 줄들이 들어 있는지 비교해요. Spring Boot 4부터는 Flyway도 `spring-boot-starter-flyway` 스타터가 필요하고, PostgreSQL용 `flyway-database-postgresql`도 있어야 해요. 아래쪽 `tasks.withType(JavaCompile)` 블록(한글 인코딩)도 지금 넣어두세요. Initializr가 넣어준 테스트 스타터(`...-test`)는 그대로 둬도 돼요.

4. **.gitignore**: 완성본의 `.gitignore`로 바꿔두세요. `.env`, `*.pem`처럼 절대 올리면 안 되는 파일이 미리 들어 있어요.

5. **설정 파일**: `src/main/resources/application.properties`를 지우고 `application.yml`을 만들어요. 이번엔 완성본에서 `(1부)` 표시된 부분만 적어요: `spring.application`, `spring.datasource`(hikari 포함), `spring.jpa`, `logging`.

6. **이번에 만들 파일**
   ```
   src/main/resources/db/migration/V1__init.sql
   src/main/java/com/dohun/nfcreview/domain/EventType.java
   src/main/java/com/dohun/nfcreview/domain/Store.java
   src/main/java/com/dohun/nfcreview/domain/Card.java
   src/main/java/com/dohun/nfcreview/domain/CardEvent.java
   ```
   파일 이름 규칙 주의: `V1__init.sql`은 `V` 대문자, 밑줄 **두 개**예요.

## 코드 읽기 포인트

- `V1__init.sql`의 컬럼과 엔티티 필드를 나란히 놓고 비교해보세요. `reviewUrl` → `review_url`처럼 자바의 카멜케이스가 DB의 스네이크케이스로 자동 연결돼요.
- `Card.store`의 `@ManyToOne(fetch = LAZY)`: 카드를 불러올 때 가게까지 매번 불러오지 않아요. 필요할 때만.
- 엔티티에 setter가 없어요. 대신 `Store.update(...)`, `Card.toggleActive()`처럼 **의미 있는 이름의 메서드**로만 값을 바꿔요. 아무 데서나 값이 바뀌는 걸 막는 습관이에요.
- `protected` 기본 생성자는 JPA 전용이에요. 우리가 실수로 `new Store()`로 빈 가게를 만드는 걸 막아줘요.

## 확인하기

1. `NfcreviewApplication` 실행 → 콘솔에 `Successfully applied 1 migration`과 비슷한 줄
2. pgAdmin에서 테이블 4개 확인: `store`, `card`, `card_event`, `flyway_schema_history`
3. `SELECT * FROM flyway_schema_history;` → V1이 적용된 기록
4. **실험**: `Store.java`의 `benefitText` 필드 이름을 `benefit`으로 바꾸고 실행 → `Schema validation: missing column [benefit]` 에러로 앱이 안 켜져요. validate가 지켜주는 걸 확인했으면 되돌리세요.

## 막히면

- `password authentication failed` → application.yml의 비밀번호
- `Found non-empty schema(s) "public" but no schema history table` → 할 일 1번(DB 비우기)
- `Cannot find a Java installation ... languageVersion=21` → JDK 21 설치, 또는 build.gradle의 21을 설치된 버전(17 이상)으로
- `Validate failed: Migration checksum mismatch` → 이미 적용된 V1 파일을 고친 경우예요. **적용된 마이그레이션은 절대 고치지 않아요.** 바꾸고 싶으면 `V2__...sql`을 새로 만들어요. (지금은 연습 중이니 DB를 비우고 다시 해도 돼요)

## 체크포인트

- Flyway가 있으면 운영 DB를 바꿀 때 어떤 점이 안전해질까?
- `ddl-auto`를 `update`가 아니라 `validate`로 둔 이유는?
- `occurred_at`을 서버 시계 그대로(LocalDateTime) 저장하면 어떤 문제가 생길까?
