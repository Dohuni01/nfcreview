# 10부. 자동 배포 (GitHub Actions)

> 목표: `main` 브랜치에 push하면 테스트 → 이미지 빌드·업로드 → 서버 반영이 자동으로 된다.

## 흐름

```
git push (main)
   │
   ▼
[test]            PostgreSQL을 잠깐 띄우고 gradlew test ── 실패하면 여기서 멈춰요
   │
   ▼
[build-and-push]  Docker 이미지 빌드 → ghcr.io(GitHub 이미지 저장소)에 업로드
   │                태그 2개: latest, 커밋 SHA
   ▼
[deploy]          SSH로 서버 접속 → git pull → 새 이미지 받기 → 컨테이너 교체
```

## 먼저 알아둘 개념

**CI / CD**: CI(지속적 통합)는 "push할 때마다 자동으로 테스트", CD(지속적 배포)는 "통과하면 자동으로 배포". 사람이 손으로 배포하면 순서를 빼먹거나 테스트를 건너뛰는 실수가 생겨요.

**테스트가 문지기**: `deploy`는 `build-and-push`를, `build-and-push`는 `test`를 기다려요(`needs`). 테스트가 하나라도 실패하면 서버는 절대 안 바뀌어요. 7부에서 만든 테스트가 여기서 일해요.

**시크릿**: 서버 IP, SSH 키 같은 값은 코드가 아니라 GitHub 저장소 설정의 Secrets에 넣고 `${{ secrets.이름 }}`으로 꺼내 써요. 로그에도 `***`로 가려져요. `GITHUB_TOKEN`은 GitHub가 실행할 때마다 자동으로 만들어주는 임시 열쇠예요.

**이미지 태그**: `latest`는 "가장 최근", 커밋 SHA 태그는 "정확히 그 커밋". 문제가 생기면 이전 SHA 이미지로 바로 되돌릴 수 있어요(롤백).

**배포 순간의 끊김**: 컨테이너를 교체하는 수십 초 동안은 접속이 안 돼요. 그래서 push는 **가게 영업이 끝난 시간에** 하는 습관을 들여요. (끊김 없는 무중단 배포는 11부 "다음 단계")

## 할 일

1. **이번에 만들 파일**: `.github/workflows/deploy.yml`
2. (좋은 습관) `gradlew`에 실행 권한을 Git에 기록해요. 윈도우 Git은 이걸 자동으로 안 해줘요:
   ```
   git update-index --chmod=+x gradlew
   git commit -m "gradlew 실행 권한"
   ```
3. GitHub 저장소 → **Settings → Secrets and variables → Actions → New repository secret**
   - `SERVER_HOST`: 탄력적 IP
   - `SERVER_USER`: `ubuntu`
   - `SERVER_SSH_KEY`: `.pem` 파일을 메모장으로 열어 `-----BEGIN`부터 `-----END ...-----`까지 **전부** 복사
4. 서버의 `deploy/.env`에서 이미지 주소를 바꿔요. 저장만 하면 돼요. 지금 돌고 있는 앱은 멈추지 않아요.
   ```
   APP_IMAGE=ghcr.io/깃허브아이디소문자/nfcreview:latest
   ```
   이미지 이름은 **소문자만** 돼요. 아이디에 대문자가 있어도 소문자로 적어요.
5. push → 저장소의 **Actions** 탭에서 진행 상황 보기

## 첫 실행에서 deploy가 실패하는 건 정상이에요

처음 올라간 이미지는 **비공개(private)**라서 서버가 받을 권한이 없어요.
1. GitHub 내 프로필 → **Packages** → `nfcreview` → **Package settings** → Change visibility → **Public**
2. Actions 탭에서 실패한 실행 → **Re-run failed jobs**

비공개로 두고 싶다면 공개 대신 서버에서 한 번 로그인해요: GitHub → Settings → Developer settings → Personal access tokens (classic) → `read:packages` 권한으로 토큰 발급 → 서버에서 `docker login ghcr.io -u 깃허브아이디` (비밀번호 자리에 토큰).

## 확인하기

1. 세 단계 모두 초록색 체크
2. `landing.html`의 안내 문구를 조금 바꿔서 push → 몇 분 뒤 폰으로 랜딩 페이지 새로고침 → 바뀐 문구
3. 일부러 테스트를 깨뜨려 push → `test`에서 멈추고 서버는 그대로인지 확인 → 되돌려서 push

## 롤백 (배포했는데 문제가 생겼다면)

1. Actions 탭에서 **문제 없던 마지막 실행**의 커밋 SHA(40자) 복사
2. 서버에서:
   ```bash
   cd ~/nfcreview/deploy
   nano .env      # APP_IMAGE=ghcr.io/아이디/nfcreview:복사한SHA
   docker compose up -d
   ```
3. 원인을 고쳐 다시 push한 뒤 `.env`를 `:latest`로 돌려놓기

## 막히면

- `denied`, `unauthorized` (deploy 단계): 위의 패키지 공개, 또는 서버에서 docker login
- `dial tcp ... i/o timeout` (deploy 단계): 보안 그룹의 22번 포트
- `ssh: handshake failed`, `unable to authenticate`: `SERVER_SSH_KEY`에 pem 내용이 전부 들어갔는지
- `git pull` 실패 (deploy 단계): 서버에서 저장소 파일을 직접 고쳤을 때 생겨요. 서버에서 `cd ~/nfcreview && git status`로 확인하고, 고친 내용은 PC에서 커밋해서 push하세요.
- test 단계에서만 실패: 로컬에서 `gradlew test`부터 통과시키기

## 체크포인트

- `needs`를 빼면 어떤 위험한 일이 생길 수 있을까?
- `latest` 태그만 쓰고 SHA 태그를 안 쓰면 롤백이 왜 어려워질까?
