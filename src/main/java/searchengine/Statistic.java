package searchengine;

import searchengine.models.Site;
import searchengine.models.Status;
import searchengine.service.LemmaRepositoryService;
import searchengine.service.PageRepositoryService;
import searchengine.service.SiteRepositoryService;
import searchengine.service.StatisticService;
import searchengine.service.statisticDTO.StatisticsDetailedDTO;
import searchengine.service.statisticDTO.StatisticsDTO;
import searchengine.service.statisticDTO.StatisticsTotalDTO;
import searchengine.service.responses.StatisticApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Statistic implements StatisticService {

    private static final Log log = LogFactory.getLog(Statistic.class);

    private final SiteRepositoryService siteRepositoryService;
    private final LemmaRepositoryService lemmaRepositoryService;
    private final PageRepositoryService pageRepositoryService;

    public Statistic(SiteRepositoryService siteRepositoryService,
                     LemmaRepositoryService lemmaRepositoryService,
                     PageRepositoryService pageRepositoryService) {
        this.siteRepositoryService = siteRepositoryService;
        this.lemmaRepositoryService = lemmaRepositoryService;
        this.pageRepositoryService = pageRepositoryService;
    }

    public StatisticApiResponse getStatistic(){
        StatisticsTotalDTO statisticsTotalDTO = getTotal();
        List<Site> siteList = siteRepositoryService.getAllSites();
        StatisticsDetailedDTO[] statisticsDetailedDTOS = new StatisticsDetailedDTO[siteList.size()];
        for (int i = 0; i < siteList.size(); i++) {
            statisticsDetailedDTOS[i] = getDetailed(siteList.get(i));
        }
        log.info("Получение статистики.");
        return new StatisticApiResponse(true, new StatisticsDTO(statisticsTotalDTO, statisticsDetailedDTOS));
    }

    private StatisticsTotalDTO getTotal(){
        long sites = siteRepositoryService.siteCount();
        long lemmas = lemmaRepositoryService.lemmaCount();
        long pages = pageRepositoryService.pageCount();
        boolean isIndexing = isSitesIndexing();
        return new StatisticsTotalDTO(sites, pages, lemmas, isIndexing);

    }

    private StatisticsDetailedDTO getDetailed(Site site){
        String url = site.getUrl();
        String name = site.getName();
        Status status = site.getStatus();
        long statusTime = site.getStatusTime().getTime();
        String error = site.getLastError();
        long pages = pageRepositoryService.pageCount(site.getId());
        long lemmas = lemmaRepositoryService.lemmaCount(site.getId());
        return new StatisticsDetailedDTO(url, name, status, statusTime, error, pages, lemmas);
    }

    private boolean isSitesIndexing(){
        boolean is = true;
        for(Site s : siteRepositoryService.getAllSites()){
            if(!s.getStatus().equals(Status.INDEXED)){
                is = false;
                break;
            }
        }
    return is;
    }
}
