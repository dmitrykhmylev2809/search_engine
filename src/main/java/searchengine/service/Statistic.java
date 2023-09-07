package searchengine.service;

import searchengine.models.Site;
import searchengine.models.Status;
import searchengine.dao.LemmaRepositoryDao;
import searchengine.dao.PageRepositoryDao;
import searchengine.dao.SiteRepositoryDao;
import searchengine.dao.StatisticDao;
import searchengine.dto.statisticDTO.StatisticsDetailedDTO;
import searchengine.dto.statisticDTO.StatisticsDTO;
import searchengine.dto.statisticDTO.StatisticsTotalDTO;
import searchengine.controllers.responses.StatisticApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Statistic implements StatisticDao {

    private static final Log log = LogFactory.getLog(Statistic.class);

    private final SiteRepositoryDao siteRepositoryDao;
    private final LemmaRepositoryDao lemmaRepositoryDao;
    private final PageRepositoryDao pageRepositoryDao;

    public Statistic(SiteRepositoryDao siteRepositoryDao,
                     LemmaRepositoryDao lemmaRepositoryDao,
                     PageRepositoryDao pageRepositoryDao) {
        this.siteRepositoryDao = siteRepositoryDao;
        this.lemmaRepositoryDao = lemmaRepositoryDao;
        this.pageRepositoryDao = pageRepositoryDao;
    }

    public StatisticApiResponse getStatistic(){
        StatisticsTotalDTO statisticsTotalDTO = getTotal();
        List<Site> siteList = siteRepositoryDao.getAllSites();
        StatisticsDetailedDTO[] statisticsDetailedDTOS = new StatisticsDetailedDTO[siteList.size()];
        for (int i = 0; i < siteList.size(); i++) {
            statisticsDetailedDTOS[i] = getDetailed(siteList.get(i));
        }
        log.info("Получение статистики.");
        return new StatisticApiResponse(true, new StatisticsDTO(statisticsTotalDTO, statisticsDetailedDTOS));
    }

    private StatisticsTotalDTO getTotal(){
        long sites = siteRepositoryDao.siteCount();
        long lemmas = lemmaRepositoryDao.lemmaCount();
        long pages = pageRepositoryDao.pageCount();
        boolean isIndexing = isSitesIndexing();
        return new StatisticsTotalDTO(sites, pages, lemmas, isIndexing);

    }

    private StatisticsDetailedDTO getDetailed(Site site){
        String url = site.getUrl();
        String name = site.getName();
        Status status = site.getStatus();
        long statusTime = site.getStatusTime().getTime();
        String error = site.getLastError();
        long pages = pageRepositoryDao.pageCount(site.getId());
        long lemmas = lemmaRepositoryDao.lemmaCount(site.getId());
        return new StatisticsDetailedDTO(url, name, status, statusTime, error, pages, lemmas);
    }

    private boolean isSitesIndexing(){
        boolean is = true;
        for(Site s : siteRepositoryDao.getAllSites()){
            if(!s.getStatus().equals(Status.INDEXED)){
                is = false;
                break;
            }
        }
    return is;
    }
}
