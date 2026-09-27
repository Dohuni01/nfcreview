-- MySQL 8 버전. TIMESTAMPTZ → TIMESTAMP(6), IDENTITY → AUTO_INCREMENT
-- 바꾸고 싶으면 V2__설명.sql 같은 새 파일을 추가해요.

-- 가게
CREATE TABLE store (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    review_url   VARCHAR(1000) NOT NULL,
    benefit_text VARCHAR(200)
);

-- NFC 카드 (카드에는 https://도메인/t/{code} 주소가 들어가요)
CREATE TABLE card (
    id       BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    store_id BIGINT      NOT NULL,
    label    VARCHAR(50),
    active   BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_card_store FOREIGN KEY (store_id) REFERENCES store (id)
);
CREATE INDEX idx_card_store ON card (store_id);

-- 기록: 태그(TAP)와 리뷰 버튼 클릭(CLICK)
CREATE TABLE card_event (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    card_id     BIGINT       NOT NULL,
    event_type  VARCHAR(10)  NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    user_agent  VARCHAR(500),
    CONSTRAINT fk_event_card FOREIGN KEY (card_id) REFERENCES card (id),
    CHECK (event_type IN ('TAP', 'CLICK'))
);
CREATE INDEX idx_card_event_card_time ON card_event (card_id, occurred_at);
