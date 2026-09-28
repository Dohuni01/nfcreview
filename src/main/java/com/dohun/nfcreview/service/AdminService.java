package com.dohun.nfcreview.service;

import com.dohun.nfcreview.domain.Card;
import com.dohun.nfcreview.domain.LandingMode;
import com.dohun.nfcreview.domain.Store;
import com.dohun.nfcreview.repository.CardEventRepository;
import com.dohun.nfcreview.repository.CardRepository;
import com.dohun.nfcreview.repository.StoreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * (5부) 관리 화면의 규칙.
 * 클래스에 readOnly = true를 기본으로 걸고, 데이터를 바꾸는 메서드에만 @Transactional을 다시 붙여요.
 * 읽기 전용 트랜잭션은 조금 더 가볍고, 실수로 값이 바뀌는 걸 막아줘요.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final StoreRepository storeRepository;
    private final CardRepository cardRepository;
    private final CardEventRepository cardEventRepository;
    private final CardCodeGenerator codeGenerator;

    public AdminService(StoreRepository storeRepository, CardRepository cardRepository,
                        CardEventRepository cardEventRepository, CardCodeGenerator codeGenerator) {
        this.storeRepository = storeRepository;
        this.cardRepository = cardRepository;
        this.cardEventRepository = cardEventRepository;
        this.codeGenerator = codeGenerator;
    }

    public List<Store> findAllStores() {
        return storeRepository.findAll(Sort.by("id"));
    }

    public Store getStore(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("가게를 찾을 수 없어요: " + id));
    }

    public List<Card> findCards(Long storeId) {
        return cardRepository.findByStoreIdOrderByIdAsc(storeId);
    }

    @Transactional
    public Long createStore(String name, String reviewUrl, LandingMode landingMode, String landingUrl) {
        return storeRepository.save(new Store(name, reviewUrl, landingMode, landingUrl)).getId();
    }

    @Transactional
    public void updateStore(Long id, String name, String reviewUrl, LandingMode landingMode, String landingUrl) {
        getStore(id).update(name, reviewUrl, landingMode, landingUrl);
        // save()를 부르지 않아도 돼요. 트랜잭션이 끝날 때 JPA가 바뀐 값을 찾아 UPDATE 해줘요(변경 감지).
    }

    @Transactional
    public String addCard(Long storeId, String label) {
        Store store = getStore(storeId);
        String code = newUniqueCode();
        cardRepository.save(new Card(code, store, label));
        return code;
    }

    @Transactional
    public void deleteStore(Long id) {
        List<Card> cards = cardRepository.findByStoreIdOrderByIdAsc(id);
        List<Long> cardIds = cards.stream().map(Card::getId).toList();
        if (!cardIds.isEmpty()) {
            cardEventRepository.deleteByCardIdIn(cardIds);
            cardRepository.deleteAll(cards);
        }
        storeRepository.deleteById(id);
    }

    /** 카드 켜기/끄기. 돌아갈 가게 화면을 알려주려고 가게 id를 돌려줘요. */
    @Transactional
    public Long toggleCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("카드를 찾을 수 없어요: " + cardId));
        card.toggleActive();
        return card.getStore().getId();
    }

    /** 카드 삭제. 이벤트 기록을 먼저 지우고 카드를 삭제해요. 돌아갈 가게 id를 돌려줘요. */
    @Transactional
    public Long deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("카드를 찾을 수 없어요: " + cardId));
        Long storeId = card.getStore().getId();
        cardEventRepository.deleteByCardId(cardId);
        cardRepository.delete(card);
        return storeId;
    }

    /** 가게의 통계 기록 전체 초기화. 카드와 가게 정보는 유지돼요. */
    @Transactional
    public void resetStats(Long storeId) {
        List<Long> cardIds = cardRepository.findByStoreIdOrderByIdAsc(storeId)
                .stream().map(Card::getId).toList();
        if (!cardIds.isEmpty()) {
            cardEventRepository.deleteByCardIdIn(cardIds);
        }
    }

    private String newUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!cardRepository.existsByCode(code)) {
                return code;
            }
        }
        // 84조 가지라 사실상 올 일 없지만, "끝없이 반복하는 코드"는 절대 만들지 않아요.
        // 동시에 같은 코드가 만들어지는 극단적인 경우는 DB의 UNIQUE 제약이 막아줘요.
        throw new IllegalStateException("카드 코드 생성에 실패했어요. 다시 시도해주세요.");
    }
}
