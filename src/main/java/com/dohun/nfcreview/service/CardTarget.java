package com.dohun.nfcreview.service;

import com.dohun.nfcreview.domain.Card;
import com.dohun.nfcreview.domain.LandingMode;
import com.dohun.nfcreview.domain.Store;

public record CardTarget(Long cardId, String code, String storeName,
                         String reviewUrl, LandingMode landingMode, String landingUrl) {

    static CardTarget from(Card card) {
        Store store = card.getStore();
        return new CardTarget(card.getId(), card.getCode(), store.getName(),
                store.getReviewUrl(), store.getLandingMode(), store.getLandingUrl());
    }
}
