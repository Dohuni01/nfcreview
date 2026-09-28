package com.dohun.nfcreview.controller;

import com.dohun.nfcreview.config.AppProperties;
import com.dohun.nfcreview.domain.LandingMode;
import com.dohun.nfcreview.service.CardTarget;
import com.dohun.nfcreview.service.NotFoundException;
import com.dohun.nfcreview.service.TapService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * (2부) 손님이 들어오는 입구. HTTP만 담당하고, 규칙은 TapService에 맡겨요.
 *
 *  /t/{code}   (t = tap)    카드 태그 → TAP 기록 → DIRECT: 리뷰 URL로 / CUSTOM: 정적 홈페이지로
 *  /go/{code}               리뷰 버튼 → CLICK 기록 → 네이버 리뷰로
 *
 * CUSTOM 랜딩 시 ?go={baseUrl}/go/{code} 를 붙여줘서 정적 페이지가 버튼 링크를 조립할 수 있게 해요.
 */
@Controller
public class TapController {

    private final TapService tapService;
    private final AppProperties appProperties;

    public TapController(TapService tapService, AppProperties appProperties) {
        this.tapService = tapService;
        this.appProperties = appProperties;
    }

    @GetMapping("/t/{code}")
    public String tap(@PathVariable("code") String code,
                      @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        CardTarget target = tapService.tap(code, userAgent).orElseThrow(() -> notFound(code));
        if (target.landingMode() == LandingMode.CUSTOM && target.landingUrl() != null) {
            String goUrl = appProperties.baseUrl() + "/go/" + target.code();
            String separator = target.landingUrl().contains("?") ? "&" : "?";
            String destination = target.landingUrl() + separator + "go=" + goUrl;
            return "redirect:" + RedirectUrls.headerSafe(destination);
        }
        return "redirect:" + RedirectUrls.headerSafe(target.reviewUrl());
    }

    @GetMapping("/go/{code}")
    public String go(@PathVariable("code") String code,
                     @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        CardTarget target = tapService.click(code, userAgent).orElseThrow(() -> notFound(code));
        return "redirect:" + RedirectUrls.headerSafe(target.reviewUrl());
    }

    private static NotFoundException notFound(String code) {
        return new NotFoundException("카드를 찾을 수 없어요: " + code);
    }
}
