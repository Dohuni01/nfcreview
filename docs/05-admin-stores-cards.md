# 5부. 가게·카드 관리 화면

> 목표: SQL 없이 화면에서 가게를 등록·수정하고, 카드를 발급하고, 카드를 끄고 켤 수 있게 한다.

## 먼저 알아둘 개념

**PRG 패턴(POST → Redirect → GET)**: 폼을 제출(POST)하면 바로 화면을 그리지 않고 다른 주소로 리다이렉트해요. 그래야 새로고침해도 "양식 다시 제출" 경고와 함께 가게가 두 번 등록되는 일이 없어요. 리다이렉트 뒤에 한 번만 보여줄 메시지는 `addFlashAttribute`로 넘겨요.

**검증 흐름**: `@Valid`가 `StoreForm`의 규칙(`@NotBlank`, `@Pattern`...)을 검사하고, 결과는 바로 뒤의 `BindingResult`에 담겨요. 에러가 있으면 입력값을 그대로 든 채 폼을 다시 보여주고, `th:errors`가 칸 밑에 메시지를 띄워요. HTML의 `required`, `maxlength`는 편의 기능일 뿐, **진짜 검증은 항상 서버에서** 해요. 브라우저 검사는 누구나 우회할 수 있거든요.

**`@InitBinder` + `StringTrimmerEditor`**: 입력값 앞뒤 공백을 자르고 빈 칸은 null로 바꿔요. 주소를 복사할 때 딸려온 공백 때문에 생기는 오류를 막아요.

**변경 감지(더티 체킹)**: `AdminService.updateStore()`에는 `save()`가 없어요. 트랜잭션 안에서 엔티티 값을 바꾸면, 트랜잭션이 끝날 때 JPA가 바뀐 부분을 찾아 UPDATE를 보내요. 콘솔의 SQL 로그로 직접 확인해보세요.

**`@Transactional(readOnly = true)`는 클래스에, 쓰기 메서드만 `@Transactional`**: 기본은 읽기 전용(조금 더 가볍고, 실수로 값이 바뀌는 걸 막아요), 데이터를 바꾸는 메서드만 따로 열어요.

**카드 코드**: `SecureRandom`으로 8자리를 만들어요. 일반 `Random`은 다음 값을 예측할 수 있어서, 누가 남의 가게 카드 주소를 알아내 통계를 부풀릴 수 있어요. 0/O, 1/l/I처럼 헷갈리는 글자는 뺐어요. 중복은 `existsByCode`로 한 번 거르고, DB의 UNIQUE 제약이 최후의 방어선이에요.

**상태를 바꾸는 요청은 POST**: "카드 끄기"를 GET 링크로 만들면 링크 미리보기 봇이나 브라우저의 미리 불러오기가 눌러버릴 수 있어요. CSRF 토큰도 POST 폼에만 붙어요.

## 할 일

**이번에 만들 파일**
```
src/main/java/.../repository/StoreRepository.java
src/main/java/.../service/CardCodeGenerator.java
src/main/java/.../service/AdminService.java
src/main/java/.../controller/form/StoreForm.java
src/main/java/.../controller/AdminStoreController.java
src/main/resources/templates/admin/stores.html
src/main/resources/templates/admin/store-form.html
src/main/resources/templates/admin/store.html
```

## 확인하기

1. 로그인 → 가게 목록 (2부에서 SQL로 넣은 빨간주막이 보여요)
2. "가게 추가" → 리뷰 주소에 `http://`로 시작하는 주소나 빈칸을 넣어 저장 → 에러 메시지 확인 → 제대로 입력해 저장
3. 가게 화면에서 "3번 테이블"로 카드 발급 → "NFC에 쓸 주소" 복사 → 새 탭에 붙여넣기 → 랜딩 페이지
4. "끄기" → 같은 주소로 접속 → 404 화면 → "켜기"로 되돌리기
5. "정보 수정"에서 혜택 문구 변경 → 랜딩 페이지에 바로 반영
6. "미리보기"는 `/l/` 주소라서 태그(TAP)가 기록되지 않아요. 단, 미리보기에서 버튼을 누르면 클릭(CLICK)은 기록되니 테스트할 때 참고하세요.
7. 가게 정보를 수정할 때 콘솔에 `update store set ...` SQL이 찍히는지 (변경 감지)
8. 통계 링크는 6부 전까지 404가 정상이에요.
9. 리뷰 주소에 한글이 든 주소를 넣어도 저장되고, 랜딩의 리뷰 버튼이 제대로 이동하는지 (2부의 `RedirectUrls`)
10. 로그인 화면이나 수정 화면을 30분 넘게 열어뒀다가 제출하면 "요청을 처리하지 못했어요"(403)가 떠요. 세션이 끝나서 CSRF 토큰이 무효가 된 거예요. 새로고침 후 다시 로그인하면 돼요.

## 체크포인트

- 등록 후 `redirect:` 없이 바로 `"admin/store"`를 돌려주면 새로고침할 때 무슨 일이 생길까?
- 브라우저 검증(`required`)이 있는데 서버 검증을 또 하는 이유는?
- 카드 코드를 1, 2, 3... 순서 번호로 만들면 어떤 문제가 생길까?
