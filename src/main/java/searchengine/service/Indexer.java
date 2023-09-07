package searchengine.service;

import org.springframework.stereotype.Service;
import searchengine.dao.SearchSettings;
import searchengine.dao.SiteIndexingDao;
import searchengine.dao.*;
import searchengine.models.Field;
import searchengine.models.Site;
import searchengine.models.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

//@Component
@Service
public class Indexer {

    private final static Log log = LogFactory.getLog(Indexer.class);

    public volatile boolean  isStopped;

    private final SearchSettings searchSettings;

    private final FieldRepositoryDao fieldRepositoryDao;
    private final SiteRepositoryDao siteRepositoryDao;
    private final IndexRepositoryDao indexRepositoryDao;
    private final PageRepositoryDao pageRepositoryDao;
    private final LemmaRepositoryDao lemmaRepositoryDao;

    List<Future<?>> futures = new ArrayList<>();

    public Indexer(SearchSettings searchSettings,
                   FieldRepositoryDao fieldRepositoryDao,
                   SiteRepositoryDao siteRepositoryDao,
                   IndexRepositoryDao indexRepositoryDao,
                   PageRepositoryDao pageRepositoryDao,
                   LemmaRepositoryDao lemmaRepositoryDao) {
        this.searchSettings = searchSettings;
        this.fieldRepositoryDao = fieldRepositoryDao;
        this.siteRepositoryDao = siteRepositoryDao;
        this.indexRepositoryDao = indexRepositoryDao;
        this.pageRepositoryDao = pageRepositoryDao;
        this.lemmaRepositoryDao = lemmaRepositoryDao;
    }



    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

    public boolean allSiteIndexing() throws InterruptedException {
        fieldInit();
        boolean isIndexing;
        List<Site> siteList = getSiteListFromConfig();
        for (Site site : siteList) {
            isIndexing = startSiteIndexing(site);
            if (!isIndexing){
                stopSiteIndexing();
                return false;
            }
        }
        return true;
    }

    public String checkedSiteIndexing(String url) throws InterruptedException {
        List<Site> siteList = siteRepositoryDao.getAllSites();
        String baseUrl = "";
        for(Site site : siteList) {
            if(site.getStatus() != Status.INDEXED) {
                return "false";
            }
            if(url.contains(site.getUrl())){
                baseUrl = site.getUrl();
            }
        }
        if(baseUrl.isEmpty()){
            return "not found";
        } else {
            Site site = siteRepositoryDao.getSite(baseUrl);
            site.setUrl(url);
            SiteIndexingDao indexing = new SiteIndexingDao(
                    site,
                    searchSettings,
                    fieldRepositoryDao,
                    siteRepositoryDao,
                    indexRepositoryDao,
                    pageRepositoryDao,
                    lemmaRepositoryDao,
                    false);
            Future<?> future = executor.submit(indexing);
            futures.add(future);
            site.setUrl(baseUrl);
            siteRepositoryDao.save(site);
            return "true";
        }
    }


    private void fieldInit() {
        Field fieldTitle = new Field("title", "title", 1.0f);
        Field fieldBody = new Field("body", "body", 0.8f);
        if (fieldRepositoryDao.getFieldByName("title") == null) {
            fieldRepositoryDao.save(fieldTitle);
            fieldRepositoryDao.save(fieldBody);
        }
    }

    private boolean startSiteIndexing(Site site) throws InterruptedException {
        Site site1 = siteRepositoryDao.getSite(site.getUrl());
        if (site1 == null) {
            siteRepositoryDao.save(site);
            SiteIndexingDao indexing = new SiteIndexingDao(
                    siteRepositoryDao.getSite(site.getUrl()),
                    searchSettings,
                    fieldRepositoryDao,
                    siteRepositoryDao,
                    indexRepositoryDao,
                    pageRepositoryDao,
                    lemmaRepositoryDao,
                    true);
            Future<?> future = executor.submit(indexing);
            futures.add(future);
            return true;
        } else {
            if (!site1.getStatus().equals(Status.INDEXING)){
                SiteIndexingDao indexing = new SiteIndexingDao(
                        siteRepositoryDao.getSite(site.getUrl()),
                        searchSettings,
                        fieldRepositoryDao,
                        siteRepositoryDao,
                        indexRepositoryDao,
                        pageRepositoryDao,
                        lemmaRepositoryDao,
                        true);
                Future<?> future = executor.submit(indexing);
                futures.add(future);

                return true;
            } else {
                return false;
            }
        }
    }

    public boolean stopSiteIndexing(){
        boolean isThreadAlive = false;
        isStopped = true;
        if(executor.getActiveCount() == 0){
            return false;
        }
        try {
        for (Future<?> future : futures) {
            future.cancel(true);
            executor.shutdownNow();
            isThreadAlive = executor.awaitTermination(1, TimeUnit.MINUTES);
        }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Ошибка закрытия потоков: " + e);
        }
        if (!isThreadAlive){
            List<Site> siteList = siteRepositoryDao.getAllSites();
            for(Site site : siteList) {
                site.setStatus(Status.FAILED);
                siteRepositoryDao.save(site);
            }
        }
        return !isThreadAlive;
    }

    private List<Site> getSiteListFromConfig() {
        List<Site> siteList = new ArrayList<>();
        List<HashMap<String, String>> sites = searchSettings.getSite();
        for (HashMap<String, String> map : sites) {
            String url = "";
            String name = "";
            for (Map.Entry<String, String> siteInfo : map.entrySet()) {
                if (siteInfo.getKey().equals("name")) {
                    name = siteInfo.getValue();
                }
                if (siteInfo.getKey().equals("url")) {
                    url = siteInfo.getValue();
                }
            }
            Site site = new Site();
            site.setUrl(url);
            site.setName(name);
            site.setStatus(Status.FAILED);
            siteList.add(site);
        }
        return siteList;
    }
}
