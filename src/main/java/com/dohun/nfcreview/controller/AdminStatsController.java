package com.dohun.nfcreview.controller;

import com.dohun.nfcreview.service.AdminService;
import com.dohun.nfcreview.service.StatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
        int period = Math.max(1, Math.min(days, 365)); // 이상한 값(0, -5, 99999)이 와도 1~365로
        model.addAttribute("store", adminService.getStore(id));
        model.addAttribute("stats", statsService.storeStats(id, period));
        model.addAttribute("periods", PERIODS);
        return "admin/stats";
    }
}
