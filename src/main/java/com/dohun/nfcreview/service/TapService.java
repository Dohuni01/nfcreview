package com.dohun.nfcreview.service;

import com.dohun.nfcreview.domain.EventType;
import com.dohun.nfcreview.domain.LandingMode;
import com.dohun.nfcreview.repository.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * (2부) 손님 흐름의 규칙을 모아둔 곳.
 *
 * 손님 경로 3원칙
 *  1. 기록이 실패해도 손님은 무조건 다음 페이지로 간다.       → recordSafely()의 try-catch
 *  2. DB가 잠깐 죽어도, 최근에 성공한 조회 결과로 보낸다.     → find()의 catch + lastKnown
 *  3. DB가 대답이 없으면 오래 기다리지 않는다.               → application.yml의 connection-timeout 3초
 */
@Service
public class TapService {

    private static final Logger log = LoggerFactory.getLogger(TapService.class);

    // 카톡 링크 미리보기, 검색엔진 같은 "사람 아닌 방문"은 통계에서 빼요
    //  yeti: 네이버 검색 로봇 / whatsapp: 왓츠앱 링크 미리보기 / headless: 화면 없는 자동화 브라우저(HeadlessChrome)
    // 주의: naver, daum, kakaotalk은 넣으면 안 돼요. 네이버·다음·카톡 앱 안에서 링크를 연 "진짜 손님"의
    //       User-Agent에도 NAVER(inapp; ...), DaumApps, KAKAOTALK 같은 글자가 들어 있어요.
    private static final Pattern BOT = Pattern.compile(
            "bot|crawl|spider|scrap|preview|facebookexternalhit|yeti|whatsapp|headless", Pattern.CASE_INSENSITIVE);

    private final CardRepository cardRepository;
    private final CardEventRecorder recorder;

    // 코드 → 마지막으로 성공한 조회 결과. 여러 요청이 동시에 읽고 쓰니까 ConcurrentHashMap.
    private final Map<String, CardTarget> lastKnown = new ConcurrentHashMap<>();

    public TapService(CardRepository cardRepository, CardEventRecorder recorder) {
        this.cardRepository = cardRepository;
        this.recorder = recorder;
    }

    /** 카드 태그: TAP 기록. DIRECT 모드는 리뷰 페이지로 바로 이동하므로 CLICK도 함께 기록 */
    public Optional<CardTarget> tap(String code, String userAgent) {
        Lookup lookup = find(code);
        if (lookup.fromDatabase()) {
            lookup.target().ifPresent(target -> {
                recordSafely(target, EventType.TAP, userAgent);
                if (target.landingMode() == LandingMode.DIRECT) {
                    recordSafely(target, EventType.CLICK, userAgent);
                }
            });
        }
        return lookup.target();
    }

    /** 리뷰 버튼: 찾고 + CLICK 기록 */
    public Optional<CardTarget> click(String code, String userAgent) {
        return findAndRecord(code, EventType.CLICK, userAgent);
    }

    private Optional<CardTarget> findAndRecord(String code, EventType type, String userAgent) {
        Lookup lookup = find(code);
        // DB 조회가 방금 실패했다면 기록도 실패할 게 뻔해요. 또 3초를 기다리게 하지 않고 건너뛰어요.
        if (lookup.fromDatabase()) {
            lookup.target().ifPresent(target -> recordSafely(target, type, userAgent));
        }
        return lookup.target();
    }

    private Lookup find(String code) {
        try {
            Optional<CardTarget> target = cardRepository.findActiveByCode(code).map(CardTarget::from);
            if (target.isPresent()) {
                lastKnown.put(code, target.get());
            } else {
                lastKnown.remove(code); // 꺼진 카드는 기억에서도 지워요 (장애 때 되살아나지 않게)
                log.warn("알 수 없는 카드: code={}", code);
            }
            return new Lookup(target, true);
        } catch (RuntimeException e) {
            // 원칙 2: DB가 안 되면 기억해둔 정보로. 기억에 없으면 어쩔 수 없이 "없는 카드" 화면.
            CardTarget remembered = lastKnown.get(code);
            log.error("카드 조회 실패 → 기억해둔 정보 사용={} code={}", remembered != null, code, e);
            return new Lookup(Optional.ofNullable(remembered), false);
        }
    }

    private void recordSafely(CardTarget target, EventType type, String userAgent) {
        if (looksLikeBot(userAgent)) {
            log.debug("봇 방문은 기록하지 않음: {}", userAgent);
            return;
        }
        try {
            recorder.record(target.cardId(), type, userAgent);
        } catch (RuntimeException e) {
            // 원칙 1: 손님은 리뷰 쓰러 온 거지 우리 통계를 도와주러 온 게 아니에요. 로그만 남기고 넘어가요.
            log.error("{} 기록 실패 (손님 이동은 정상 진행) code={}", type, target.code(), e);
        }
    }

    static boolean looksLikeBot(String userAgent) {
        return userAgent != null && BOT.matcher(userAgent).find();
    }

    // 조회 결과 + "DB에서 방금 가져온 건지(true) / 기억에서 꺼낸 건지(false)"
    private record Lookup(Optional<CardTarget> target, boolean fromDatabase) {
    }
}
