package com.dohun.nfcreview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * 가게 한 곳 = store 테이블의 한 줄.
 */
@Entity
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 1000)
    private String reviewUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LandingMode landingMode;

    // CUSTOM 모드일 때 NFC 탭 후 보여줄 정적 페이지 주소
    @Column(length = 1000)
    private String landingUrl;

    protected Store() {
    }

    public Store(String name, String reviewUrl, LandingMode landingMode, String landingUrl) {
        this.name = name;
        this.reviewUrl = reviewUrl;
        this.landingMode = landingMode;
        this.landingUrl = landingUrl;
    }

    public void update(String name, String reviewUrl, LandingMode landingMode, String landingUrl) {
        this.name = name;
        this.reviewUrl = reviewUrl;
        this.landingMode = landingMode;
        this.landingUrl = landingUrl;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getReviewUrl() { return reviewUrl; }
    public LandingMode getLandingMode() { return landingMode; }
    public String getLandingUrl() { return landingUrl; }
}
