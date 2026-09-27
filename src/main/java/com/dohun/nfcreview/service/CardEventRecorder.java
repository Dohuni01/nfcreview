package com.dohun.nfcreview.service;

import com.dohun.nfcreview.domain.Card;
import com.dohun.nfcreview.domain.CardEvent;
import com.dohun.nfcreview.domain.EventType;
import com.dohun.nfcreview.repository.CardEventRepository;
import com.dohun.nfcreview.repository.CardRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * (2부) 기록 한 건을 저장해요. record() 호출 하나가 트랜잭션 하나예요.
 *
 * TapService와 다른 클래스로 뺀 이유:
 * 스프링의 @Transactional은 "다른 빈이 호출할 때"만 적용돼요. 스프링이 빈을 대리 객체(프록시)로 감싸서
 * 바깥에서 들어오는 호출을 가로채는 방식이라, 같은 클래스 안에서 this.record()로 부르면 트랜잭션이 안 걸려요.
 */
@Component
public class CardEventRecorder {

    private final CardRepository cardRepository;
    private final CardEventRepository cardEventRepository;

    public CardEventRecorder(CardRepository cardRepository, CardEventRepository cardEventRepository) {
        this.cardRepository = cardRepository;
        this.cardEventRepository = cardEventRepository;
    }

    @Transactional
    public void record(Long cardId, EventType type, String userAgent) {
        // getReferenceById: 카드를 SELECT하지 않고 id만 든 대리 객체를 줘요. 저장에는 id만 있으면 되니까 쿼리 하나 절약.
        Card cardRef = cardRepository.getReferenceById(cardId);
        cardEventRepository.save(new CardEvent(cardRef, type, userAgent));
    }
}
