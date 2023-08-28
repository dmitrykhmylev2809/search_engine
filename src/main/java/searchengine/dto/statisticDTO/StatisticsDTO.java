package searchengine.dto.statisticDTO;

public class StatisticsDTO {
    StatisticsTotalDTO statisticsTotalDTO;
    StatisticsDetailedDTO[] statisticsDetailedDTO;

    public StatisticsDTO(StatisticsTotalDTO statisticsTotalDTO, StatisticsDetailedDTO[] statisticsDetailedDTO) {
        this.statisticsTotalDTO = statisticsTotalDTO;
        this.statisticsDetailedDTO = statisticsDetailedDTO;
    }

    public StatisticsTotalDTO getTotal() {
        return statisticsTotalDTO;
    }

    public void setTotal(StatisticsTotalDTO statisticsTotalDTO) {
        this.statisticsTotalDTO = statisticsTotalDTO;
    }

    public StatisticsDetailedDTO[] getDetailed() {
        return statisticsDetailedDTO;
    }

    public void setDetailed(StatisticsDetailedDTO[] statisticsDetailedDTO) {
        this.statisticsDetailedDTO = statisticsDetailedDTO;
    }
}
