package searchengine.component;

import org.springframework.stereotype.Component;
import searchengine.config.SearchSettings;
import searchengine.repo.*;
import searchengine.url.SiteIndexing;
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

@Component
public class Indexer {

    private final static Log log = LogFactory.getLog(Indexer.class);

    public volatile boolean  isStopped;

    private final SearchSettings searchSettings;

    private FieldRepository fieldRepository;
    private final SiteRepository siteRepository;
    private IndexRepository indexRepository;
    private PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    List<Future<?>> futures = new ArrayList<>();

    public Indexer(SearchSettings searchSettings,
                   FieldRepository fieldRepository,
                   SiteRepository siteRepository,
                   IndexRepository indexRepository,
                   PageRepository pageRepository,
                   LemmaRepository lemmaRepository) {
        this.searchSettings = searchSettings;
        this.fieldRepository = fieldRepository;
        this.siteRepository = siteRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
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
        List<Site> siteList = siteRepository.findAll();
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
            Site site = siteRepository.findByUrl(baseUrl);
            site.setUrl(url);
            SiteIndexing indexing = new SiteIndexing(
                    site,
                    searchSettings,
                    fieldRepository,
                    siteRepository,
                    indexRepository,
                    pageRepository,
                    lemmaRepository,
                    false);
            Future<?> future = executor.submit(indexing);
            futures.add(future);
            site.setUrl(baseUrl);
            siteRepository.save(site);
            return "true";
        }
    }


    private void fieldInit() {
        Field fieldTitle = new Field("title", "title", 1.0f);
        Field fieldBody = new Field("body", "body", 0.8f);
        if (fieldRepository.getFieldByName("title") == null) {
            fieldRepository.save(fieldTitle);
            fieldRepository.save(fieldBody);
        }
    }

    private boolean startSiteIndexing(Site site) throws InterruptedException {
        Site site1 = siteRepository.findByUrl(site.getUrl());
        if (site1 == null) {
            siteRepository.save(site);
            SiteIndexing indexing = new SiteIndexing(
                    siteRepository.findByUrl(site.getUrl()),
                    searchSettings,
                    fieldRepository,
                    siteRepository,
                    indexRepository,
                    pageRepository,
                    lemmaRepository,
                    true);
            Future<?> future = executor.submit(indexing);
            futures.add(future);
            return true;
        } else {
            if (!site1.getStatus().equals(Status.INDEXING)){
                SiteIndexing indexing = new SiteIndexing(
                        siteRepository.findByUrl(site.getUrl()),
                        searchSettings,
                        fieldRepository,
                        siteRepository,
                        indexRepository,
                        pageRepository,
                        lemmaRepository,
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
            List<Site> siteList = siteRepository.findAll();
            for(Site site : siteList) {
                site.setStatus(Status.FAILED);
                siteRepository.save(site);
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
