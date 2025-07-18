package olsh.backend.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import olsh.backend.api_gateway.dto.response.StatisticsResponse;
import olsh.backend.api_gateway.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "https://open-labs-share.online"}, allowCredentials = "true",
        maxAge = 3600)
@Tag(name = "Statistics", description = "Endpoints for retrieving statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    @Operation(summary = "Get statistics", description = "Retrieves statistics data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = StatisticsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Statistics not found")
    })
    public ResponseEntity<StatisticsResponse> getStatistics() {
        StatisticsResponse response = statisticsService.getStatistics();
        return ResponseEntity.ok(response);
    }
}
