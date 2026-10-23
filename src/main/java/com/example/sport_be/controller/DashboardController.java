package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.dashboard.CategoryRevenueResponse;
import com.example.sport_be.dto.dashboard.DailyRevenueResponse;
import com.example.sport_be.dto.dashboard.DashboardSummaryResponse;
import com.example.sport_be.dto.dashboard.TopProductResponse;
import com.example.sport_be.service.DashboardService;
import com.example.sport_be.service.DashboardOverviewService;
import com.example.sport_be.dto.dashboard.DashboardOverviewResponse;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only endpoints for the Dashboard overview (Requirement 10).
 *
 * <p>Access is restricted to {@code ROLE_ADMIN} by {@code SecurityConfig},
 * which requires that role for every route under {@code /api/admin/**}.
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardOverviewService overviewService;

    public DashboardController(DashboardService dashboardService, DashboardOverviewService overviewService) {
        this.dashboardService = dashboardService;
        this.overviewService = overviewService;
    }

    @GetMapping("/api/admin/dashboard/overview")
    public ResponseEntity<DashboardOverviewResponse> overview(@RequestParam(defaultValue = "7days") String range,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(overviewService.get(range, from, to));
    }

    @GetMapping(ApiPaths.Dashboard.SUMMARY)
    public ResponseEntity<DashboardSummaryResponse> getSummary(@RequestParam String range) {
        return ResponseEntity.ok(dashboardService.getSummary(range));
    }

    @GetMapping(ApiPaths.Dashboard.TOP_PRODUCTS)
    public ResponseEntity<List<TopProductResponse>> getTopProducts(@RequestParam String range) {
        return ResponseEntity.ok(dashboardService.getTopProducts(range));
    }

    @GetMapping(ApiPaths.Dashboard.REVENUE_BY_DAY)
    public ResponseEntity<List<DailyRevenueResponse>> getRevenueByDay(@RequestParam String range) {
        return ResponseEntity.ok(dashboardService.getRevenueByDay(range));
    }

    @GetMapping(ApiPaths.Dashboard.REVENUE_BY_CATEGORY)
    public ResponseEntity<List<CategoryRevenueResponse>> getRevenueByCategory(@RequestParam String range) {
        return ResponseEntity.ok(dashboardService.getRevenueByCategory(range));
    }
}
