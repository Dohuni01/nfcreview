package com.dohun.nfcreview;

import com.dohun.nfcreview.domain.Card;
import com.dohun.nfcreview.domain.EventType;
import com.dohun.nfcreview.domain.LandingMode;
import com.dohun.nfcreview.domain.Store;
import com.dohun.nfcreview.repository.CardEventRepository;
import com.dohun.nfcreview.repository.CardRepository;
import com.dohun.nfcreview.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * (7부) 통합 테스트: 진짜 스프링 + 진짜 DB로 손님 흐름 전체를 요청 단위로 확인해요.
 * 로컬에선 application.yml의 DB, GitHub Actions에선 잠깐 띄운 MySQL을 써요.
 *
 * MockMvc: 서버를 띄우지 않고 가짜 HTTP 요청을 보내는 도구. Spring Boot 4부터는 @AutoConfigureMockMvc가 필요해요.
 * Transactional: 테스트가 끝나면 DB 변경을 전부 되돌려요(롤백). DB에 테스트 데이터가 쌓이지 않아요.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TapFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    CardRepository cardRepository;

    @Autowired
    CardEventRepository cardEventRepository;

    @BeforeEach
    void setUp() {
        Store store = storeRepository.save(new Store("통합테스트가게", "https://example.com/review", LandingMode.DIRECT, null));
        cardRepository.save(new Card("itest001", store, "테스트 카드"));
    }

    @Test
    void DIRECT_모드_태그하면_TAP과_CLICK을_동시에_기록하고_리뷰_URL로_보낸다() throws Exception {
        mockMvc.perform(get("/t/itest001").header("User-Agent", "Mozilla/5.0 Test"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/review"));

        assertThat(cardEventRepository.countByCardCodeAndEventType("itest001", EventType.TAP)).isEqualTo(1);
        assertThat(cardEventRepository.countByCardCodeAndEventType("itest001", EventType.CLICK)).isEqualTo(1);
    }

    @Test
    void CUSTOM_모드_태그하면_TAP을_기록하고_정적_홈페이지로_보낸다() throws Exception {
        Store customStore = storeRepository.save(new Store("커스텀가게",
                "https://example.com/naver-review", LandingMode.CUSTOM, "https://example.com/landing"));
        cardRepository.save(new Card("itest003", customStore, "커스텀 카드"));

        mockMvc.perform(get("/t/itest003").header("User-Agent", "Mozilla/5.0 Test"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/landing"));

        assertThat(cardEventRepository.countByCardCodeAndEventType("itest003", EventType.TAP)).isEqualTo(1);
    }

    @Test
    void 리뷰_버튼은_CLICK을_기록하고_리뷰_주소로_보낸다() throws Exception {
        mockMvc.perform(get("/go/itest001").header("User-Agent", "Mozilla/5.0 Test"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/review"));

        assertThat(cardEventRepository.countByCardCodeAndEventType("itest001", EventType.CLICK)).isEqualTo(1);
    }

    @Test
    void 리뷰_주소에_한글이나_중괄호가_있어도_인코딩해서_보낸다() throws Exception {
        Store store = storeRepository.save(new Store("한글주소가게",
                "https://m.search.naver.com/search.naver?query=빨간주막&x={foo}", LandingMode.DIRECT, null));
        cardRepository.save(new Card("itest002", store, null));

        mockMvc.perform(get("/go/itest002").header("User-Agent", "Mozilla/5.0 Test"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(
                        "https://m.search.naver.com/search.naver?query=%EB%B9%A8%EA%B0%84%EC%A3%BC%EB%A7%89&x=%7Bfoo%7D"));
    }

    @Test
    void 없는_카드는_404() throws Exception {
        mockMvc.perform(get("/t/no-such-card"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 관리자_화면은_로그인해야_들어간다() throws Exception {
        mockMvc.perform(get("/admin/stores"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", endsWith("/admin/login")));
    }
}
