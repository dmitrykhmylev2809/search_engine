package searchengine.service.responses;


import searchengine.service.statisticDTO.StatisticsDTO;

public class StatisticApiResponse implements ApiResponse {
    boolean result;
    StatisticsDTO statisticsDTO;

    public StatisticApiResponse(boolean result, StatisticsDTO statisticsDTO) {
        this.result = result;
        this.statisticsDTO = statisticsDTO;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public StatisticsDTO getStatistics() {
        return statisticsDTO;
    }

    public void setStatistics(StatisticsDTO statisticsDTO) {
        this.statisticsDTO = statisticsDTO;
    }
}
