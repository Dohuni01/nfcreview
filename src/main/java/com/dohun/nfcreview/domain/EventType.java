package com.dohun.nfcreview.domain;

/**
 * 카드에서 일어난 일의 종류.
 * TAP: 손님이 카드를 태그했다 / CLICK: 랜딩 페이지에서 리뷰 버튼을 눌렀다
 */
public enum EventType {
    TAP,
    CLICK
}
