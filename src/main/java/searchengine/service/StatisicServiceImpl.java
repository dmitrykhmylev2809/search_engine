package searchengine.service;

import searchengine.models.Site;
import searchengine.models.Status;
import searchengine.dto.statisticDTO.StatisticsDetailedDTO;
import searchengine.dto.statisticDTO.StatisticsDTO;
import searchengine.dto.statisticDTO.StatisticsTotalDTO;
import searchengine.controllers.responses.StatisticApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import searchengine.repo.LemmaRepository;
import searchengine.repo.PageRepository;
import searchengine.repo.SiteRepository;

import java.util.List;

@Service
public class StatisicServiceImpl implements StatisticService {

    private static final Log log = LogFactory.getLog(StatisicServiceImpl.class);

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    public StatisicServiceImpl(SiteRepository siteRepository,
                               LemmaRepository lemmaRepository,
                               PageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
    }

    public StatisticApiResponse getStatistic(){
        StatisticsTotalDTO statisticsTotalDTO = getTotal();
        List<Site> siteList = siteRepository.findAll();
        StatisticsDetailedDTO[] statisticsDetailedDTOS = new StatisticsDetailedDTO[siteList.size()];
        for (int i = 0; i < siteList.size(); i++) {
            statisticsDetailedDTOS[i] = getDetailed(siteList.get(i));
        }
        log.info("Получение статистики.");
        return new StatisticApiResponse(true, new StatisticsDTO(statisticsTotalDTO, statisticsDetailedDTOS));
    }

    private StatisticsTotalDTO getTotal(){
        long sites = siteRepository.count();
        long lemmas = lemmaRepository.count();
        long pages = pageRepository.count();
        boolean isIndexing = isSitesIndexing();
        return new StatisticsTotalDTO(sites, pages, lemmas, isIndexing);

    }

    private StatisticsDetailedDTO getDetailed(Site site){
        String url = site.getUrl();
        String name = site.getName();
        Status status = site.getStatus();
        long statusTime = site.getStatusTime().getTime();
        String error = site.getLastError();
        long pages = pageRepository.countBySiteId(site.getId());
        long lemmas = lemmaRepository.countBySiteId(site.getId());
        return new StatisticsDetailedDTO(url, name, status, statusTime, error, pages, lemmas);
    }

    private boolean isSitesIndexing(){
        boolean is = true;
        for(Site s : siteRepository.findAll()){
            if(!s.getStatus().equals(Status.INDEXED)){
                is = false;
                break;
            }
        }
    return is;
    }
}
