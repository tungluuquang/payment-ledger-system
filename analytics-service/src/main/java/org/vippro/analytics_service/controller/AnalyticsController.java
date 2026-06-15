package org.vippro.analytics_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vippro.analytics_service.dto.AnalyticsOverviewResponse;
import org.vippro.analytics_service.service.AnalyticsQueryService;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    @GetMapping("/overview")
    public AnalyticsOverviewResponse overview(
            @RequestParam(defaultValue = "24") int hours
    ) {
        return queryService.overview(hours);
    }
}
