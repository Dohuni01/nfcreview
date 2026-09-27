package com.dohun.nfcreview.repository;

import com.dohun.nfcreview.domain.CardEvent;
import com.dohun.nfcreview.domain.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

// (2부) 기록 저장용
public interface CardEventRepository extends JpaRepository<CardEvent, Long> {

    long countByCardCodeAndEventType(String code, EventType eventType);

    void deleteByCardIdIn(Collection<Long> cardIds);
}
