package com.dohun.nfcreview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * NFC 카드 한 장 = card 테이블의 한 줄.
 * 카드 칩에는 "https://내도메인/t/{code}" 주소가 들어가요.
 */
@Entity
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주소 끝에 붙는 카드 고유 코드. 겹치면 안 되니까 DB에도 UNIQUE 제약을 걸어뒀어요.
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    // 카드 여러 장 → 가게 하나 (N:1). LAZY: 가게 정보는 정말 필요할 때만 가져와요.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // "3번 테이블", "카운터" 같은 위치 메모 → 카드별 통계가 곧 위치별 통계가 돼요
    @Column(length = 50)
    private String label;

    // 잃어버렸거나 계약이 끝난 카드는 꺼요. 꺼진 카드를 태그하면 "없는 카드" 화면이 나와요.
    @Column(nullable = false)
    private boolean active = true;

    protected Card() {
    }

    public Card(String code, Store store, String label) {
        this.code = code;
        this.store = store;
        this.label = label;
    }

    public void toggleActive() {
        this.active = !this.active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Store getStore() {
        return store;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return active;
    }
}
