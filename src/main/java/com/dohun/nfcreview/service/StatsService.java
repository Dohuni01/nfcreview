package com.dohun.nfcreview.service;

import com.dohun.nfcreview.domain.EventType;
import com.dohun.nfcreview.repository.CardEventStatsRepository;
import com.dohun.nfcreview.repository.CardEventStatsRepository.CardCountRow;
import com.dohun.nfcreview.repository.CardEventStatsRepository.DailyCountRow;
import com.dohun.nfcreview.repository.CardEventStatsRepository.HeatmapRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (6부) 통계 계산. 세는 일은 DB가 하고(GROUP BY), 여기서는 모양을 다듬어요.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    // 저장은 UTC, 통계는 "한국 날짜" 기준
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CardEventStatsRepository statsRepository;

    public StatsService(CardEventStatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    public StoreStats storeStats(Long storeId, int days) {
        LocalDate today = LocalDate.now(KST);
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant since = firstDay.atStartOfDay(KST).toInstant(); // 한국 시간 firstDay 00:00

        // 1) 날짜별: DB가 센 결과를 "날짜 → 개수" 지도로 옮겨요
        Map<LocalDate, Long> tapsByDay = new HashMap<>();
        Map<LocalDate, Long> clicksByDay = new HashMap<>();
        for (DailyCountRow row : statsRepository.countDaily(storeId, since)) {
            LocalDate day = LocalDate.parse(row.getDay());
            Map<LocalDate, Long> target = EventType.TAP.name().equals(row.getType()) ? tapsByDay : clicksByDay;
            target.merge(day, row.getCnt(), Long::sum);
        }

        // 2) 빈 날짜도 0으로 채워요. 태그가 없던 날이 표에서 빠지면 추세를 잘못 읽게 돼요.
        long maxTaps = tapsByDay.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<DailyStat> daily = new ArrayList<>();
        for (LocalDate day = today; !day.isBefore(firstDay); day = day.minusDays(1)) { // 최근 날짜가 위로
            long taps = tapsByDay.getOrDefault(day, 0L);
            long clicks = clicksByDay.getOrDefault(day, 0L);
            daily.add(new DailyStat(day, taps, clicks, percent(taps, maxTaps)));
        }

        // 3) 카드별
        List<CardStat> cards = new ArrayList<>();
        for (CardCountRow row : statsRepository.countByCard(storeId, since)) {
            long taps = row.getTaps();
            long clicks = row.getClicks();
            cards.add(new CardStat(row.getCode(), row.getLabel(), row.getActive(),
                    taps, clicks, percent(clicks, taps)));
        }

        // 4) 히트맵: 요일(0=월…6=일) × 시간(0~23) TAP 건수
        int[][] heatmap = new int[7][24];
        for (HeatmapRow row : statsRepository.countHeatmap(storeId, since)) {
            heatmap[row.getDow()][row.getHr()] = row.getCnt().intValue();
        }

        long totalTaps = daily.stream().mapToLong(DailyStat::taps).sum();
        long totalClicks = daily.stream().mapToLong(DailyStat::clicks).sum();
        return new StoreStats(days, totalTaps, totalClicks, percent(totalClicks, totalTaps), daily, cards, heatmap);
    }

    // part ÷ whole 을 0~100 정수 퍼센트로. 0으로 나누는 경우를 막아요.
    static int percent(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }

    // 화면에 넘길 결과들. record라서 Thymeleaf에서 ${stats.taps}처럼 바로 꺼내 써요.
    public record StoreStats(int days, long taps, long clicks, int conversionPercent,
                             List<DailyStat> daily, List<CardStat> cards, int[][] heatmap) {
    }

    public record DailyStat(LocalDate day, long taps, long clicks, int barPercent) {
    }

    public record CardStat(String code, String label, boolean active,
                           long taps, long clicks, int conversionPercent) {
    }
}
