package com.dohun.nfcreview;

import com.dohun.nfcreview.domain.Card;
import com.dohun.nfcreview.domain.LandingMode;
import com.dohun.nfcreview.domain.Store;
import com.dohun.nfcreview.repository.CardRepository;
import com.dohun.nfcreview.repository.StoreRepository;
import com.dohun.nfcreview.service.StatsService.StoreStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * (7부) 통합 테스트: 관리 화면(가게·카드·통계)과 보안 규칙.
 *
 * WithMockUser: 로그인 과정을 건너뛰고 "ADMIN으로 로그인한 상태"를 만들어요.
 * csrf(): 진짜 화면의 폼처럼 CSRF 토큰을 붙여요. 빼면 403이 나야 정상이에요.
 * 통계 SQL(AT TIME ZONE, FILTER)은 PostgreSQL 전용이라, 진짜 DB로 돌리는 이 테스트가 SQL 오타까지 잡아줘요.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminFlowIntegrationTest {

    private static final String BROWSER = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    CardRepository cardRepository;

    @Test
    void 로그인했어도_CSRF_토큰이_없으면_거부한다() throws Exception {
        mockMvc.perform(post("/admin/stores")
                        .param("name", "가게")
                        .param("reviewUrl", "https://example.com/review"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 가게를_등록하면_앞뒤_공백을_자르고_가게_화면으로_보낸다() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/stores").with(csrf())
                        .param("name", "  관리테스트가게  ")
                        .param("reviewUrl", " https://example.com/review ")
                        .param("landingMode", "DIRECT"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("/admin/stores/*"))
                .andReturn();

        Long id = idFrom(result.getResponse().getRedirectedUrl());
        Store saved = storeRepository.findById(id).orElseThrow();
        assertThat(saved.getName()).isEqualTo("관리테스트가게");
        assertThat(saved.getReviewUrl()).isEqualTo("https://example.com/review");
        assertThat(saved.getLandingMode()).isEqualTo(LandingMode.DIRECT);
    }

    @Test
    void 잘못된_입력은_저장하지_않고_에러와_함께_폼을_다시_보여준다() throws Exception {
        long before = storeRepository.count();

        mockMvc.perform(post("/admin/stores").with(csrf())
                        .param("name", "   ")
                        .param("reviewUrl", "http://example.com/review"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/store-form"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "reviewUrl"));

        assertThat(storeRepository.count()).isEqualTo(before);
    }

    @Test
    void 카드를_발급하고_끄면_손님_주소가_404가_된다() throws Exception {
        Store store = storeRepository.save(new Store("카드테스트가게", "https://example.com/review", LandingMode.DIRECT, null));

        mockMvc.perform(post("/admin/stores/{id}/cards", store.getId()).with(csrf()).param("label", "3번 테이블"))
                .andExpect(status().isFound());
        List<Card> cards = cardRepository.findByStoreIdOrderByIdAsc(store.getId());
        assertThat(cards).hasSize(1);
        Card card = cards.get(0);
        assertThat(card.getCode()).hasSize(8);
        assertThat(card.getLabel()).isEqualTo("3번 테이블");

        mockMvc.perform(get("/t/{code}", card.getCode()).header("User-Agent", BROWSER))
                .andExpect(status().isFound());

        mockMvc.perform(post("/admin/cards/{id}/toggle", card.getId()).with(csrf()))
                .andExpect(status().isFound());
        mockMvc.perform(get("/t/{code}", card.getCode()).header("User-Agent", BROWSER))
                .andExpect(status().isNotFound());
    }

    @Test
    void 통계는_사람의_태그와_클릭만_센다() throws Exception {
        Store store = storeRepository.save(new Store("통계테스트가게", "https://example.com/review", LandingMode.DIRECT, null));
        cardRepository.save(new Card("stest001", store, "1번 테이블"));
        cardRepository.save(new Card("stest002", store, "2번 테이블")); // 한 번도 안 쓴 카드

        // DIRECT 모드: /t/ 한 번이 TAP + CLICK 동시 기록
        mockMvc.perform(get("/t/stest001").header("User-Agent", BROWSER));
        mockMvc.perform(get("/t/stest001").header("User-Agent", BROWSER));
        mockMvc.perform(get("/t/stest001").header("User-Agent", "kakaotalk-scrap/1.0")); // 봇: 안 셈

        MvcResult result = mockMvc.perform(get("/admin/stores/{id}/stats", store.getId()).param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/stats"))
                .andReturn();

        StoreStats stats = (StoreStats) result.getModelAndView().getModel().get("stats");
        assertThat(stats.taps()).isEqualTo(2);
        assertThat(stats.clicks()).isEqualTo(2);                 // DIRECT: tap = click
        assertThat(stats.conversionPercent()).isEqualTo(100);
        assertThat(stats.daily()).hasSize(7);                    // 기록 없는 날도 0으로 채워서 7줄
        assertThat(stats.daily().get(0).taps()).isEqualTo(2);    // 맨 위 = 오늘
        assertThat(stats.cards()).hasSize(2);                    // 안 쓴 카드도 나와야 해요 (LEFT JOIN)
        assertThat(stats.cards().get(1).taps()).isZero();
    }

    private static Long idFrom(String redirectedUrl) {
        return Long.valueOf(redirectedUrl.substring(redirectedUrl.lastIndexOf('/') + 1));
    }
}
