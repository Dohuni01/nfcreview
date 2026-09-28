package com.dohun.nfcreview.controller;

import com.dohun.nfcreview.domain.Store;
import com.dohun.nfcreview.service.AdminService;
import com.dohun.nfcreview.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** (6부) 가게별 통계 화면 */
@Controller
public class AdminStatsController {

    private static final List<Integer> PERIODS = List.of(7, 30, 90);

    private final AdminService adminService;
    private final StatsService statsService;

    public AdminStatsController(AdminService adminService, StatsService statsService) {
        this.adminService = adminService;
        this.statsService = statsService;
    }

    @GetMapping("/admin/stores/{id}/stats")
    public String stats(@PathVariable("id") Long id,
                        @RequestParam(name = "days", defaultValue = "30") int days,
                        Model model) {
        int period = Math.max(1, Math.min(days, 365));
        model.addAttribute("store", adminService.getStore(id));
        model.addAttribute("stats", statsService.storeStats(id, period));
        model.addAttribute("periods", PERIODS);
        return "admin/stats";
    }

    @PostMapping("/admin/stores/{id}/stats/reset")
    public String resetStats(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        adminService.resetStats(id);
        redirectAttributes.addFlashAttribute("message", "통계를 초기화했어요.");
        return "redirect:/admin/stores/" + id + "/stats";
    }

    @GetMapping("/admin/stores/{id}/stats/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable("id") Long id,
                                             @RequestParam(name = "days", defaultValue = "30") int days) {
        Store store = adminService.getStore(id);
        int period = Math.max(1, Math.min(days, 365));
        StatsService.StoreStats stats = statsService.storeStats(id, period);

        StringBuilder sb = new StringBuilder();
        // Excel UTF-8 BOM
        sb.append('﻿');
        sb.append("날짜,태그,리뷰버튼\n");
        for (StatsService.DailyStat row : stats.daily()) {
            sb.append(row.day()).append(",").append(row.taps()).append(",").append(row.clicks()).append("\n");
        }
        sb.append("\n위치,코드,태그,리뷰버튼,전환율\n");
        for (StatsService.CardStat card : stats.cards()) {
            String label = card.label() != null ? escapeCsv(card.label()) : "(메모 없음)";
            sb.append(label).append(",")
              .append(card.code()).append(",")
              .append(card.taps()).append(",")
              .append(card.clicks()).append(",")
              .append(card.conversionPercent()).append("%\n");
        }

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = store.getName() + "_통계_최근" + period + "일.csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encoded)
                .body(body);
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
