package searchengine.controllers;

import searchengine.service.StatisticService;
import searchengine.controllers.responses.StatisticApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatisticController {

    private final StatisticService statisticService;

    public StatisticController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    @GetMapping("/api/statistics")
    public ResponseEntity<Object> getStatistics(){
        StatisticApiResponse apiResponse = statisticService.getStatistic();
        return ResponseEntity.ok (apiResponse);
    }
}
