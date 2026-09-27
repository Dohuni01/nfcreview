package com.dohun.nfcreview.service;

import com.dohun.nfcreview.domain.Card;
import com.dohun.nfcreview.domain.EventType;
import com.dohun.nfcreview.domain.LandingMode;
import com.dohun.nfcreview.domain.Store;
import com.dohun.nfcreview.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * (7부) 단위 테스트: DB 없이 TapService의 규칙("손님 경로 3원칙")만 빠르게 검사해요.
 * DB 대신 가짜 객체(@Mock)를 넣어서 "저장이 실패했다면", "DB가 죽었다면" 같은 상황을 마음대로 만들어요.
 */
@ExtendWith(MockitoExtension.class)
class TapServiceTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    CardEventRecorder recorder;

    @InjectMocks // 위의 가짜 객체들을 생성자에 넣어서 진짜 TapService를 만들어요
    TapService tapService;

    private Card sampleCard() {
        Store store = new Store("테스트가게", "https://example.com/review", LandingMode.DIRECT, null);
        return new Card("abc12345", store, "1번 테이블");
    }

    @Test
    void 없는_카드면_빈_결과이고_기록도_안_한다() {
        // given
        when(cardRepository.findActiveByCode("nope")).thenReturn(Optional.empty());

        // when
        Optional<CardTarget> result = tapService.tap("nope", "Mozilla/5.0");

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(recorder);
    }

    @Test
    void 기록이_실패해도_이동할_곳은_돌려준다() {
        when(cardRepository.findActiveByCode("abc12345")).thenReturn(Optional.of(sampleCard()));
        doThrow(new RuntimeException("저장 실패")).when(recorder).record(any(), eq(EventType.TAP), any());

        Optional<CardTarget> result = tapService.tap("abc12345", "Mozilla/5.0");

        assertThat(result).isPresent();
        assertThat(result.get().storeName()).isEqualTo("테스트가게");
    }

    @Test
    void DB가_죽으면_기억해둔_정보로_보내고_기록은_건너뛴다() {
        when(cardRepository.findActiveByCode("abc12345"))
                .thenReturn(Optional.of(sampleCard()))           // 첫 번째 호출: 정상
                .thenThrow(new RuntimeException("DB 연결 실패"));   // 두 번째 호출: 장애

        tapService.tap("abc12345", "Mozilla/5.0");                // 정상일 때 기억해둠
        Optional<CardTarget> duringOutage = tapService.tap("abc12345", "Mozilla/5.0");

        assertThat(duringOutage).isPresent();
        assertThat(duringOutage.get().reviewUrl()).isEqualTo("https://example.com/review");
        verify(recorder, times(1)).record(any(), eq(EventType.TAP), any()); // 장애 중엔 기록 시도 안 함
    }

    @Test
    void 봇_방문은_이동은_시키되_기록하지_않는다() {
        when(cardRepository.findActiveByCode("abc12345")).thenReturn(Optional.of(sampleCard()));

        Optional<CardTarget> result = tapService.tap("abc12345", "kakaotalk-scrap/1.0");

        assertThat(result).isPresent();
        verifyNoInteractions(recorder);
    }

    @Test
    void 봇_판별_규칙() {
        assertThat(TapService.looksLikeBot("Googlebot/2.1")).isTrue();
        assertThat(TapService.looksLikeBot("facebookexternalhit/1.1")).isTrue();
        assertThat(TapService.looksLikeBot("Mozilla/5.0 (compatible; Yeti/1.1; +https://naver.me/spd)")).isTrue(); // 네이버 검색 로봇
        assertThat(TapService.looksLikeBot("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)")).isFalse();
        assertThat(TapService.looksLikeBot(null)).isFalse();
    }

    @Test
    void 네이버_카톡_앱_안에서_연_손님은_봇이_아니다() {
        // 앱 안의 브라우저(인앱 브라우저)는 User-Agent 끝에 앱 이름을 붙여요. 이 손님들을 봇으로 빼면 통계가 크게 줄어요.
        assertThat(TapService.looksLikeBot(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) "
                        + "Mobile/15E148 NAVER(inapp; search; 2000; 12.10.1; 15PRO)")).isFalse();
        assertThat(TapService.looksLikeBot(
                "Mozilla/5.0 (Linux; Android 15; SM-S928N) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/130.0.0.0 Mobile Safari/537.36 KAKAOTALK 10.9.0")).isFalse();
        assertThat(TapService.looksLikeBot(
                "Mozilla/5.0 (Linux; Android 15; SM-S928N) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "SamsungBrowser/27.0 Chrome/125.0.0.0 Mobile Safari/537.36")).isFalse();
    }
}
