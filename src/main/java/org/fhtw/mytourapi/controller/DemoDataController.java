package org.fhtw.mytourapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.fhtw.mytourapi.dto.DemoDataResultDto;
import org.fhtw.mytourapi.service.DemoDataService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo-data")
@Tag(name = "Demo data", description = "Seed presentation-ready demo tours and logs for the authenticated user.")
public class DemoDataController {

    private final DemoDataService demoDataService;

    public DemoDataController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/seed")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Replace all tours and logs for the authenticated user with demo data.")
    public DemoDataResultDto seedDemoData() {
        return demoDataService.seedDemoData();
    }
}
