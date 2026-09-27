package com.dohun.nfcreview.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** (7부) 리다이렉트 주소 안전 처리 규칙 */
class RedirectUrlsTest {

    @Test
    void 평범한_주소는_그대로_둔다() {
        String url = "https://m.place.naver.com/restaurant/1317526908/review/visitor?entry=nfc&x=1#top";
        assertThat(RedirectUrls.headerSafe(url)).isEqualTo(url);
    }

    @Test
    void 한글은_UTF8_퍼센트_인코딩한다() {
        assertThat(RedirectUrls.headerSafe("https://m.search.naver.com/search.naver?query=빨간주막"))
                .isEqualTo("https://m.search.naver.com/search.naver?query=%EB%B9%A8%EA%B0%84%EC%A3%BC%EB%A7%89");
    }

    @Test
    void 이미_인코딩된_주소는_두_번_인코딩하지_않는다() {
        String url = "https://m.search.naver.com/search.naver?query=%EB%B9%A8%EA%B0%84";
        assertThat(RedirectUrls.headerSafe(url)).isEqualTo(url); // %25EB... 가 되면 안 돼요
    }

    @Test
    void 중괄호와_공백도_인코딩한다() {
        assertThat(RedirectUrls.headerSafe("https://example.com/r?x={code} y"))
                .isEqualTo("https://example.com/r?x=%7Bcode%7D%20y");
    }
}
