package searchengine.controllers;

import searchengine.dao.StatisticDao;
import searchengine.controllers.responses.StatisticApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatisticController {

    private final StatisticDao statisticDao;

    public StatisticController(StatisticDao statisticDao) {
        this.statisticDao = statisticDao;
    }

    @GetMapping("/api/statistics")
    public ResponseEntity<Object> getStatistics(){
        StatisticApiResponse apiResponse = statisticDao.getStatistic();
        return ResponseEntity.ok (apiResponse);
    }
}
