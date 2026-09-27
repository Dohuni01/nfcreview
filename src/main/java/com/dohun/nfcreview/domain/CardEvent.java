package com.dohun.nfcreview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * 기록 한 건 = card_event 테이블의 한 줄. 모든 통계는 이 테이블에서 나와요.
 */
@Entity
public class CardEvent {

    private static final int USER_AGENT_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    // DB에 'TAP', 'CLICK' 글자로 저장해요.
    // 기본값(ORDINAL)은 0, 1 순서 번호라서, 나중에 enum 순서만 바꿔도 과거 데이터의 뜻이 뒤바뀌어요.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EventType eventType;

    // 세계 기준 시각(UTC). 서버가 어느 시간대에 있든 정확해요. 한국 날짜로 바꾸는 건 통계에서 해요.
    @Column(nullable = false)
    private Instant occurredAt;

    // 어떤 폰/브라우저인지 (아이폰·안드로이드 비율 등). 개인정보가 될 수 있는 IP는 일부러 저장하지 않아요.
    @Column(length = USER_AGENT_MAX)
    private String userAgent;

    protected CardEvent() {
    }

    public CardEvent(Card card, EventType eventType, String userAgent) {
        this.card = card;
        this.eventType = eventType;
        this.occurredAt = Instant.now();
        this.userAgent = cut(userAgent);
    }

    // User-Agent가 가끔 아주 길게 와요. 칸(500자)보다 길면 저장이 실패하니까 미리 잘라요.
    private static String cut(String value) {
        if (value == null || value.length() <= USER_AGENT_MAX) {
            return value;
        }
        return value.substring(0, USER_AGENT_MAX);
    }

    public Long getId() {
        return id;
    }

    public Card getCard() {
        return card;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
