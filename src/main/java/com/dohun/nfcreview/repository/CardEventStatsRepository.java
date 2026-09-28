package com.dohun.nfcreview.repository;

import com.dohun.nfcreview.domain.CardEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * (6부) 통계 전용 저장소. 읽기만 해요.
 * MySQL 전용 문법(CONVERT_TZ, DATE_FORMAT, CASE WHEN)이 필요해서 SQL을 직접 써요(nativeQuery).
 * 결과는 아래 인터페이스로 받는데, SQL 별칭(AS day)과 getter 이름(getDay)이 같아야 연결돼요.
 */
public interface CardEventStatsRepository extends Repository<CardEvent, Long> {

    // 날짜(한국 날짜) × 종류별 개수. CONVERT_TZ로 UTC → KST(+09:00) 변환
    @Query(value = """
            SELECT DATE_FORMAT(CONVERT_TZ(e.occurred_at, '+00:00', '+09:00'), '%Y-%m-%d') AS day,
                   e.event_type AS type,
                   COUNT(*) AS cnt
            FROM card_event e
            JOIN card c ON c.id = e.card_id
            WHERE c.store_id = :storeId
              AND e.occurred_at >= :since
            GROUP BY 1, 2
            """, nativeQuery = true)
    List<DailyCountRow> countDaily(@Param("storeId") Long storeId, @Param("since") Instant since);

    // 카드별 개수. 카드 기준 LEFT JOIN이라 기록이 0건인 카드도 결과에 나와요.
    @Query(value = """
            SELECT c.code   AS code,
                   c.label  AS label,
                   c.active AS active,
                   COUNT(CASE WHEN e.event_type = 'TAP'   THEN 1 END) AS taps,
                   COUNT(CASE WHEN e.event_type = 'CLICK' THEN 1 END) AS clicks
            FROM card c
            LEFT JOIN card_event e
                   ON e.card_id = c.id
                  AND e.occurred_at >= :since
            WHERE c.store_id = :storeId
            GROUP BY c.id, c.code, c.label, c.active
            ORDER BY c.id
            """, nativeQuery = true)
    List<CardCountRow> countByCard(@Param("storeId") Long storeId, @Param("since") Instant since);

    interface DailyCountRow {
        String getDay();   // "2026-09-24"
        String getType();  // "TAP" 또는 "CLICK"
        Long getCnt();
    }

    // 요일(KST) × 시간(KST) × TAP 건수. 히트맵 렌더링에 써요.
    @Query(value = """
            SELECT WEEKDAY(CONVERT_TZ(e.occurred_at, '+00:00', '+09:00')) AS dow,
                   HOUR(CONVERT_TZ(e.occurred_at, '+00:00', '+09:00'))    AS hr,
                   COUNT(*) AS cnt
            FROM card_event e
            JOIN card c ON c.id = e.card_id
            WHERE c.store_id = :storeId
              AND e.occurred_at >= :since
              AND e.event_type = 'TAP'
            GROUP BY 1, 2
            """, nativeQuery = true)
    List<HeatmapRow> countHeatmap(@Param("storeId") Long storeId, @Param("since") Instant since);

    interface CardCountRow {
        String getCode();
        String getLabel();
        Boolean getActive();
        Long getTaps();
        Long getClicks();
    }

    interface HeatmapRow {
        Integer getDow();  // 0=월…6=일
        Integer getHr();   // 0~23 KST
        Long getCnt();
    }
}
